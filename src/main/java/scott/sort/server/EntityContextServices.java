package scott.sort.server;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.Environment;
import scott.sort.api.core.IEntityContextServices;
import scott.sort.api.core.QueryBatcher;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.AquireConnectionException;
import scott.sort.api.exception.RollbackWithoutTransactionException;
import scott.sort.api.exception.DatabaseAccessError;
import scott.sort.api.exception.RollbackException;
import scott.sort.api.exception.SetAutoCommitException;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.query.SortQueryException;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.database.Database;
import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.persister.Persister;
import scott.sort.server.jdbc.persister.exception.SortPersistException;
import scott.sort.server.jdbc.queryexecution.QueryExecuter;
import scott.sort.server.jdbc.queryexecution.QueryExecution;
import scott.sort.server.jdbc.queryexecution.QueryResult;
import scott.sort.server.jdbc.resources.ConnectionResources;

/**
 * Server implementation of entity context services
 * @author scott
 *
 */
public class EntityContextServices implements IEntityContextServices {

    private Environment env;

    private final DataSource dataSource;

    private final List<Database> databases = new LinkedList<>();

    public EntityContextServices(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void addDatabases(Database ...databases) {
        if (databases != null && databases.length > 0) {
            this.databases.addAll(Arrays.asList(databases));
        }
    }

    public void setEnvironment(Environment env) {
        this.env = env;
    }

    @Override
    public Definitions getDefinitions(String namespace) {
        return env.getDefinitions(namespace);
    }

    /**
     * newContext will share the same connection resources as context
     */
    @Override
    public void joinTransaction(EntityContext newContext, EntityContext context) {
        ConnectionResources conRes = ConnectionResources.get(context);
        if (conRes != null) {
            ConnectionResources.set(newContext, conRes.getConnection(), conRes.getDatabase());
        }
    }

    @Override
    public void setAutocommit(EntityContext entityContext, boolean value) throws SortJdbcException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        if (conRes == null) {
            /*
             * Create a new connection with the required autocommit value
             */
            conRes = newConnectionResources(entityContext, value);
        }
        else {
            /*
             * If the connection was in transactional mode and we just set it to autocommit mode
             * then the connection will automatically perform a commit.
             */
            try {
                conRes.getConnection().setAutoCommit(value);
            } catch (SQLException x) {
                throw new SetAutoCommitException("Error setting autocommit to '" + value + "'", x);
            }
        }
    }


    @Override
    public boolean getAutocommit(EntityContext entityContext) throws SortJdbcException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        if (conRes == null) {
            return true;
        }
        try {
            return conRes.getConnection().getAutoCommit();
        }
        catch (SQLException x) {
            throw new SortJdbcException("AccessError calling connection get auto commit", x);
        }
    }

    @Override
    public void rollback(EntityContext entityContext) throws SortJdbcException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        if (conRes != null) {
            try {
                if (conRes.getConnection().getAutoCommit()) {
                    throw new RollbackWithoutTransactionException("Cannot rollback when the connection is in autocommit mode");
                }
            }
            catch (SQLException x) {
                throw new DatabaseAccessError("Database access error calling connection getAutoCommit before perfoming rollback");
            }
            try {
                conRes.getConnection().rollback();
            }
            catch (SQLException x) {
                throw new RollbackException("SQLException while performing rollback", x);
            }
        }
    }

    @Override
    public <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws SortJdbcException, SortQueryException {
        env.preProcess(query, entityContext.getDefinitions());
        QueryExecution<T> execution = new QueryExecution<T>(entityContext, query, env.getDefinitions(namespace));

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool)){
            QueryExecuter executer = new QueryExecuter(conRes.getDatabase(), con.getConnection(), entityContext);
            executer.execute(execution);
            return execution.getResult();
        }
    }

    @Override
    public QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws SortJdbcException, SortQueryException {
        QueryExecution<?> queryExecutions[] = new QueryExecution[queryBatcher.size()];
        int i = 0;
        for (QueryObject<?> queryObject : queryBatcher.getQueries()) {
            env.preProcess(queryObject, entityContext.getDefinitions());
            queryExecutions[i++] = new QueryExecution<>(entityContext, queryObject, env.getDefinitions(namespace));
        }

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool);) {
            QueryExecuter exec = new QueryExecuter(conRes.getDatabase(), con.getConnection(), entityContext);
            exec.execute(queryExecutions);
            for (i = 0; i < queryExecutions.length; i++) {
                queryBatcher.addResult(queryExecutions[i].getResult());
            }
            return queryBatcher;
        }
    }

    protected Persister newPersister(Environment env, String namespace) {
        return new Persister(env, namespace);
    }

    @Override
    public PersistAnalyser execute(PersistAnalyser persistAnalyser) throws SortJdbcException, SortPersistException {
        Persister persister = newPersister(env, persistAnalyser.getEntityContext().getNamespace());
        EntityContext entityContext = persistAnalyser.getEntityContext();

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, false);
            returnToPool = true;
        }

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool);) {
            try {
                persister.persist(persistAnalyser);
                con.getConnection().commit();
                return persistAnalyser;
            }
            catch(SQLException x) {
                rollback(con.getConnection(), "Error rolling back the persist request");
                throw new SortJdbcException("Error commiting the transaction", x);
            }
            catch(SortPersistException x) {
                rollback(con.getConnection(), "Error rolling back the persist request");
                throw x;
            }
        }
    }

    private static void rollback(Connection con, String message) throws SortJdbcException {
        try {
            con.rollback();
        }
        catch(SQLException x2) {
            throw new SortJdbcException(message, x2);
        }
    }

    private ConnectionResources newConnectionResources(EntityContext entityContext, boolean autocommit) throws SortJdbcException {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException x) {
            throw new AquireConnectionException("Could not get connection from data source", x);
        }
        try {
            connection.setAutoCommit(autocommit);
        } catch (SQLException x) {
            throw new SetAutoCommitException("SQLException setting auto commit", x);
        }
        Database database = getDatabaseInfo( connection );
        ConnectionResources cr = ConnectionResources.set(entityContext, connection, database);
        return cr;
    }


    private Database getDatabaseInfo(Connection connection) throws SortJdbcException {
        DatabaseMetaData metaData;
        try {
            metaData = connection.getMetaData();
        } catch (SQLException x) {
            throw new SortJdbcException("Could not get database metadata", x);
        }
        for (Database db: databases) {
            try {
                if (db.matches(metaData)) {
                    return db;
                }
            }
            catch (SQLException x) {
                throw new SortJdbcException("Error getting databaseInfo", x);
            }
        }
        try {
            throw new SortJdbcException("Database is not supported: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new SortJdbcException("Database access error on getting database metadata");
        }
    }

}

class OptionalyClosingResources implements AutoCloseable {
    private final ConnectionResources conRes;
    private boolean reallyClose;

    public OptionalyClosingResources(ConnectionResources conRes, boolean reallyClose) {
        this.conRes = conRes;
        this.reallyClose = reallyClose;
    }

    @Override
    public void close() throws SortJdbcException  {
        if (reallyClose) {
            try {
                conRes.removeFromEntityContexts();
                conRes.getConnection().close();
            }
            catch(SQLException x) {
                throw new SortJdbcException("Error closing connection", x);
            }
        }
    }

    public Connection getConnection() {
        return conRes.getConnection();
    }

}
