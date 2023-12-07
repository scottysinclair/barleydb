package scott.barleydb.server.jdbc;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.audit.AuditInformation;
import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.DefinitionsSet;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.IEntityContextServices;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.jdbc.AquireConnectionException;
import scott.barleydb.api.exception.execution.jdbc.ClosingConnectionException;
import scott.barleydb.api.exception.execution.jdbc.CommitException;
import scott.barleydb.api.exception.execution.jdbc.CommitWithoutTransactionException;
import scott.barleydb.api.exception.execution.jdbc.DatabaseAccessError;
import scott.barleydb.api.exception.execution.jdbc.RollbackException;
import scott.barleydb.api.exception.execution.jdbc.RollbackWithoutTransactionException;
import scott.barleydb.api.exception.execution.jdbc.SetAutoCommitException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.persist.IllegalPersistStateException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.stream.EntityData;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.stream.ObjectGraph.NodeId;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.api.stream.QueryResultItem;
import scott.barleydb.server.jdbc.converter.TypeConverter;
import scott.barleydb.server.jdbc.persist.Persister;
import scott.barleydb.server.jdbc.persist.SequenceGenerator;
import scott.barleydb.server.jdbc.query.QueryExecuter;
import scott.barleydb.server.jdbc.query.QueryExecution;
import scott.barleydb.server.jdbc.query.QueryGenerator;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.server.jdbc.vendor.Database;

/**
 * JDBC implementation of entity context services which queries and persists using JDBC.
 * @author scott
 *
 */
public class JdbcEntityContextServices implements IEntityContextServices {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcEntityContextServices.class);
    private static final Logger LOG_PERSIST_REPORT = LoggerFactory.getLogger(PersistAnalyser.class.getName() + ".report");

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
    public void close(EntityContext entityContext) throws SortServiceProviderException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        if (conRes != null) {
            try {
                conRes.close();
            }
            catch(SQLException x) {
                throw new ClosingConnectionException("Error closing connection", x);
            }
        }
    }

    public <T> QueryEntityDataInputStream streamQuery(EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortJdbcException, BarleyDBQueryException {
        env.preProcess(query, entityContext.getDefinitions());

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        QueryExecution<T> execution = new QueryExecution<T>(this, entityContext, query, env.getDefinitions(entityContext.getNamespace()));

        try{
            QueryExecuter executer = new QueryExecuter(this, conRes, entityContext, props, returnToPool);
            /*
             * convert the result stream to a full in-memory result
             */
            return executer.execute(execution);
        }
        catch(EntityStreamException x) {
            throw new BarleyDBQueryException("Error processing entity stream", x);
        }
    }

    @Override
    public <T> QueryResult<T> execute(EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortJdbcException, BarleyDBQueryException {
        env.preProcess(query, entityContext.getDefinitions());

        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }

        QueryExecution<T> execution = new QueryExecution<T>(this, entityContext, query, env.getDefinitions(entityContext.getNamespace()));

        try (OptionalyClosingResources con = new OptionalyClosingResources(conRes, returnToPool)){
            QueryExecuter executer = new QueryExecuter(this, conRes, entityContext, props, returnToPool);
            /*
             * convert the result stream to a full in-memory result
             */
            return toQueryResult(entityContext, executer.execute(execution), execution);
        }
        catch(EntityStreamException x) {
            throw new BarleyDBQueryException("Error processing entity stream", x);
        }
    }

    @Override
    public QueryBatcher execute(EntityContext entityContext, QueryBatcher queryBatcher, RuntimeProperties props) throws SortJdbcException, BarleyDBQueryException {
        if (queryBatcher.getQueries().isEmpty()) {
            return queryBatcher;
        }
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
            QueryExecuter exec = new QueryExecuter(this, conRes, entityContext, props, returnToPool);
            return toQueryBatchResult(entityContext, queryBatcher, exec.execute(queryExecutions), queryExecutions);
        }
        catch(EntityStreamException x) {
            throw new BarleyDBQueryException("Error processing entity stream", x);
        }
    }

    protected Persister newPersister(Environment env, String namespace) {
        return new Persister(env, namespace, this);
    }

    public AuditInformation compareWithDatabase(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortJdbcException, SortPersistException  {
        PersistAnalyser analyser = new PersistAnalyser(persistRequest.getEntityContext());
        try (OptionalyClosingResources con = newOptionallyClosingConnection(persistRequest.getEntityContext())) {
            analyser.analyse(persistRequest);
        }
        /*
         * We can optionally copy the data to  be persisted to a new context
         * This way we only apply the changes back if the whole persist succeeds.
         */
        if (runtimeProperties.getExecuteInSameContext() == null || !runtimeProperties.getExecuteInSameContext()) {
            analyser = analyser.deepCopy();
        }
        if (LOG_PERSIST_REPORT.isDebugEnabled()) {
            LOG_PERSIST_REPORT.debug(analyser.report());
        }

        Persister persister = newPersister(env, analyser.getEntityContext().getNamespace());
        EntityContext entityContext = analyser.getEntityContext();
        if (entityContext.isUser()) {
            throw new IllegalPersistStateException("EntityContext must be set to internal.");
        }

        try (OptionalyClosingResources con = newOptionallyClosingConnection(entityContext)) {
            try {
                return persister.compareWithDatabase(analyser);
            }
            catch(SortPersistException x) {
                rollback(con.getConnection(), "Error rolling back the persist request");
                throw x;
            }
        }
    }

    @Override
    public PersistAnalyser execute(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortJdbcException, SortPersistException {
        LOG.debug("");
        LOG.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        LOG.debug(".............................................................................................");
        LOG.debug("STARTING PERSIST REQUEST EXECUTION                                                          .");
        LOG.debug(".............................................................................................");
        LOG.debug("");
        PersistAnalyser analyser = new PersistAnalyser(persistRequest.getEntityContext());
        try (OptionalyClosingResources con = newOptionallyClosingConnection(persistRequest.getEntityContext())) {
            analyser.analyse(persistRequest);
        }
        /*
         * We can optionally copy the data to  be persisted to a new context
         * This way we only apply the changes back if the whole persist succeeds.
         */
        if (runtimeProperties.getExecuteInSameContext() == null || !runtimeProperties.getExecuteInSameContext()) {
            analyser = analyser.deepCopy();
        }
        if (LOG_PERSIST_REPORT.isDebugEnabled()) {
            LOG_PERSIST_REPORT.debug(analyser.report());
        }

        Persister persister = newPersister(env, analyser.getEntityContext().getNamespace());
        EntityContext entityContext = analyser.getEntityContext();
        if (entityContext.isUser()) {
            throw new IllegalPersistStateException("EntityContext must be set to internal.");
        }

        try (OptionalyClosingResources con = newOptionallyClosingConnection(entityContext)) {
            try {
                persister.persist(analyser);
                return analyser;
            }
            catch(SortPersistException x) {
              if (!getAutoCommit(entityContext)) {
                rollback(con.getConnection(), "Error rolling back the persist request");
              }
              throw x;
            }
        }
    }

    /**
     * convert the data stream to a full in memory result.
     * @param entityContext
     * @param in
     * @return
     * @throws EntityStreamException
     */
    private <T> QueryResult<T> toQueryResult(EntityContext entityContext, QueryEntityDataInputStream in, QueryExecution<?> queryExecution) throws EntityStreamException {
        LOG.debug("Consuming QueryEntityDataInputStream and generating a QueryResult...");
        QueryResult<T> result = new QueryResult<>(entityContext);
        QueryResultItem qitem;
        Definitions defs = entityContext.getDefinitions();
        Set<Entity> alreadyAddedToResult = new HashSet<>();
        while( (qitem = in.read()) != null) {
            LOG.debug("START PROCESSING QUERY RESULT ITEM FROM STEAM.");
            List<Entity> entities = new LinkedList<>();
            for (EntityData entityData:  qitem.getObjectGraph().getEntityData()) {
            	Entity newE = entityContext.addEntityLoadedFromDB( entityData, qitem.getObjectGraph().getQueryObject(entityData) );
                entities.add( newE );
                Entity firstEntityProcessed = entities.get(0);
                if (entities.size() == 1 && !alreadyAddedToResult.contains(firstEntityProcessed)) {
                    result.getEntityList().add( firstEntityProcessed );
                    alreadyAddedToResult.add( firstEntityProcessed );
                }
            }
            for (NodeId nodeId: qitem.getObjectGraph().getFetchedToManyNodes()) {
                EntityType entityType = defs.getEntityTypeMatchingInterface( nodeId.getEntityType(), true);
                Entity entity = entityContext.getEntity(entityType, nodeId.getEntityKey(), true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).setFetched(true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).refresh();
            }
            LOG.debug("END PROCESSING QUERY RESULT ITEM FROM STEAM.");
        }
        return result;
    }

    private QueryBatcher toQueryBatchResult(EntityContext entityContext, QueryBatcher queryBatcher, QueryEntityDataInputStream in, QueryExecution<?> queryExecutions[]) throws EntityStreamException {
        LOG.debug("Consuming QueryEntityDataInputStream and generating a QueryResult...");
        for (int i=0, n=queryBatcher.getQueries().size(); i<n; i++) {
            queryBatcher.addResult( new QueryResult<>(entityContext) );
        }

        QueryResultItem qitem;
        Definitions defs = entityContext.getDefinitions();
        while( (qitem = in.read()) != null) {

            List<Entity> entities = new LinkedList<>();
            for (EntityData entityData:  qitem.getObjectGraph().getEntityData()) {
                entities.add( entityContext.addEntityLoadedFromDB( entityData,  qitem.getObjectGraph().getQueryObject(entityData)));
                if (entities.size() == 1) {
                    queryBatcher.getResults().get( qitem.getQueryIndex() ).getEntityList().add( entities.get(0));
                }
            }
            for (NodeId nodeId: qitem.getObjectGraph().getFetchedToManyNodes()) {
                EntityType entityType = defs.getEntityTypeMatchingInterface( nodeId.getEntityType(), true);
                Entity entity = entityContext.getEntity(entityType, nodeId.getEntityKey(), true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).setFetched(true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).refresh();
            }
        }
        return queryBatcher;
    }

	private OptionalyClosingResources newOptionallyClosingConnection(EntityContext entityContext) throws SortJdbcException {
        ConnectionResources conRes = ConnectionResources.get(entityContext);
        boolean returnToPool = false;
        if (conRes == null) {
            //there was no connection associated with the ctx, so we are in autocommit mode
            conRes = newConnectionResources(entityContext, true);
            returnToPool = true;
        }
        return new OptionalyClosingResources(conRes, returnToPool);
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


    public Database getDatabaseInfo(Connection connection) throws SortJdbcException {
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

	public String debugQueryString(QueryObject<Object> query, String namespace) {
		try {
			QueryGenerator qGen = new QueryGenerator(databases.get(0), query, env.getDefinitionsSet().getDefinitions(namespace));
	        return qGen.generateSQL(null, new LinkedList<>());
		}
		catch(Exception x) {
			x.printStackTrace(System.err);
			return null;
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
