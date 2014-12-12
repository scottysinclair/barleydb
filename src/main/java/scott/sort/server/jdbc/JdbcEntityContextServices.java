package scott.sort.server.jdbc;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.DefinitionsSet;
import scott.sort.api.core.Environment;
import scott.sort.api.core.IEntityContextServices;
import scott.sort.api.core.QueryBatcher;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.SortServiceProviderException;
import scott.sort.api.exception.execution.jdbc.AquireConnectionException;
import scott.sort.api.exception.execution.jdbc.ClosingConnectionException;
import scott.sort.api.exception.execution.jdbc.CommitException;
import scott.sort.api.exception.execution.jdbc.CommitWithoutTransactionException;
import scott.sort.api.exception.execution.jdbc.DatabaseAccessError;
import scott.sort.api.exception.execution.jdbc.RollbackException;
import scott.sort.api.exception.execution.jdbc.RollbackWithoutTransactionException;
import scott.sort.api.exception.execution.jdbc.SetAutoCommitException;
import scott.sort.api.exception.execution.jdbc.SortJdbcException;
import scott.sort.api.exception.execution.persist.SortPersistException;
import scott.sort.api.exception.execution.query.SortQueryException;
import scott.sort.api.persist.PersistAnalyser;
import scott.sort.api.persist.PersistRequest;
import scott.sort.api.query.QueryObject;
import scott.sort.api.query.RuntimeProperties;
import scott.sort.server.jdbc.converter.TypeConverter;
import scott.sort.server.jdbc.persist.Persister;
import scott.sort.server.jdbc.persist.SequenceGenerator;
import scott.sort.server.jdbc.query.QueryExecuter;
import scott.sort.server.jdbc.query.QueryExecution;
import scott.sort.server.jdbc.query.QueryResult;
import scott.sort.server.jdbc.resources.ConnectionResources;
import scott.sort.server.jdbc.vendor.Database;

/**
 * JDBC implementation of entity context services which queries and persists using JDBC.
 * @author scott
 *
 */
public class JdbcEntityContextServices implements IEntityContextServices {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcEntityContextServices.class);

    private Environment env;

    private final DataSource dataSource;

    private final List<Database> databases = new LinkedList<>();

    private final Map<String,TypeConverter> typeConverters;

    private SequenceGenerator sequenceGenerator;

    public JdbcEntityContextServices(DataSource dataSource) {
        this.dataSource = dataSource;
        this.typeConverters = new HashMap<>();
    }

    public void register(TypeConverter ...converters) {
        for (TypeConverter tc: converters) {
            typeConverters.put(tc.getClass().getName(), tc);
        }
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

    public SequenceGenerator getSequenceGenerator() {
        return sequenceGenerator;
    }

    public void setSequenceGenerator(SequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public DefinitionsSet getDefinitionsSet() {
        return env.getDefinitionsSet();
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
    public void setAutoCommit(EntityContext entityContext, boolean value) throws SortJdbcException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);

        if (conRes == null) {
            /*
             * If there is no associated connection and we want autocommit on, then there is nothing to do.
             */
            if (value) {
                return;
            }
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
            }
            catch (SQLException x) {
                throw new SetAutoCommitException("Error setting autocommit to '" + value + "'", x);
            }
            /*
             * If we are setting autocommit to true, then we can release the connection
             */
            if (value) {
                try {
                    LOG.debug("Releasing the jdbc connection as auto commit set to true");
                    conRes.close();
                }
                catch (SQLException x) {
                    throw new ClosingConnectionException("Error closing connection after set auto commit (true)", x);
                }
            }
        }
    }


    @Override
    public boolean getAutoCommit(EntityContext entityContext) throws SortJdbcException {
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
    public void rollback(EntityContext entityContext) throws SortServiceProviderException {
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
    public void commit(EntityContext entityContext) throws SortServiceProviderException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        if (conRes != null) {
            try {
                if (conRes.getConnection().getAutoCommit()) {
                    throw new CommitWithoutTransactionException("Cannot commit when the connection is in autocommit mode");
                }
            }
            catch (SQLException x) {
                throw new DatabaseAccessError("Database access error calling connection getAutoCommit before perfoming commit");
            }
            try {
                LOG.debug("Comitting jdbc connection");
                conRes.getConnection().commit();
                /*
                 * We assume that we can release the connection
                 * once we have comitted it.
                 */
                LOG.debug("Releasing the jdbc connection due to successfull commit");
                try {
                    conRes.close();
                }
                catch(SQLException x) {
                    throw new ClosingConnectionException("Error closing connection after commit", x);
                }
            }
            catch (SQLException x) {
                throw new CommitException("SQLException while performing commit", x);
            }
        }
    }

    @Override
    public <T> QueryResult<T> execute(EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortJdbcException, SortQueryException {
        env.preProcess(query, entityContext.getDefinitions());

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        QueryExecution<T> execution = new QueryExecution<T>(this, entityContext, query, env.getDefinitions(entityContext.getNamespace()));

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool)){
            QueryExecuter executer = new QueryExecuter(this, conRes.getDatabase(), con.getConnection(), entityContext, props);
            executer.execute(execution);
            return execution.getResult();
        }
    }

    @Override
    public QueryBatcher execute(EntityContext entityContext, QueryBatcher queryBatcher, RuntimeProperties props) throws SortJdbcException, SortQueryException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        QueryExecution<?> queryExecutions[] = new QueryExecution[queryBatcher.size()];
        int i = 0;
        for (QueryObject<?> queryObject : queryBatcher.getQueries()) {
            env.preProcess(queryObject, entityContext.getDefinitions());
            queryExecutions[i++] = new QueryExecution<>(this, entityContext, queryObject, env.getDefinitions(entityContext.getNamespace()));
        }

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool);) {
            QueryExecuter exec = new QueryExecuter(this, conRes.getDatabase(), con.getConnection(), entityContext, props);
            exec.execute(queryExecutions);
            for (i = 0; i < queryExecutions.length; i++) {
                queryBatcher.addResult(queryExecutions[i].getResult());
            }
            return queryBatcher;
        }
    }

    protected Persister newPersister(Environment env, String namespace) {
        return new Persister(env, namespace, this);
    }

    @Override
    public PersistAnalyser execute(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortJdbcException, SortPersistException {
        PersistAnalyser analyser = new PersistAnalyser(persistRequest.getEntityContext());
        analyser.analyse(persistRequest);
        /*
         * We can optionally copy the data to  be persisted to a new context
         * This way we only apply the changes back if the whole persist succeeds.
         */
        if (runtimeProperties.getExecuteInSameContext() == null || !runtimeProperties.getExecuteInSameContext()) {
            analyser = analyser.deepCopy();
        }
        if (LOG.isDebugEnabled()) {
        	LOG.debug("Persist Analysis Results: \n{}", analyser.report());
        }

        Persister persister = newPersister(env, analyser.getEntityContext().getNamespace());
        EntityContext entityContext = analyser.getEntityContext();

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, false);
            returnToPool = true;
        }

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool);) {
            try {
                persister.persist(analyser);
                con.getConnection().commit();
                return analyser;
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

    public TypeConverter getTypeConverter(String typeConverterFqn) {
        return typeConverters.get(typeConverterFqn);
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
                conRes.close();
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
