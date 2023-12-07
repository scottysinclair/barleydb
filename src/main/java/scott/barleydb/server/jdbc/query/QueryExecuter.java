package scott.barleydb.server.jdbc.query;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.persist.PreparingPersistStatementException;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.stream.ObjectGraph;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.query.QueryGenerator.Param;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.server.jdbc.vendor.Database;
import scott.barleydb.server.jdbc.query.QueryExecuter;
import scott.barleydb.server.jdbc.query.QueryExecution;
import scott.barleydb.server.jdbc.query.QueryGenerator;
import scott.barleydb.server.jdbc.query.QueryPreparedStatementHelper;

/**
 * Executes a set of QueryExecutions against the database.
 *
 * Will batch the QueryExecutions into one statement and process the multiple resultsets
 * if the database supports it.
 *
 * @author scott
 *
 */
public class QueryExecuter {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecuter.class);

    private JdbcEntityContextServices jdbcEntityContextServices;
    private final ConnectionResources connectionResources;
    private final Connection connection;
    private final EntityContext entityContext;
    private final Database database;
    private final RuntimeProperties runtimeProperties;
    private final boolean closeConnection;

    public QueryExecuter(JdbcEntityContextServices jdbcEntityContextServices, ConnectionResources connectionResources, EntityContext entityContext, RuntimeProperties runtimeProperties, boolean closeConnection)  {
        this.jdbcEntityContextServices = jdbcEntityContextServices;
        this.connectionResources = connectionResources;
        this.database = connectionResources.getDatabase();
        this.connection = connectionResources.getConnection();
        this.entityContext = entityContext;
        this.runtimeProperties = runtimeProperties;
        this.closeConnection = closeConnection;
    }

    /**
     * Executes the given query executions#
     *
     * If possible, in one database roundtrip.
     * @param queryExecutions
     * @throws PreparingPersistStatementException
     * @throws BarleyDBQueryException
     * @throws EntityStreamException
     * @throws SQLException
     * @throws Exception
     */
    public QueryEntityDataInputStream execute(QueryExecution<?>... queryExecutions) throws SortJdbcException, BarleyDBQueryException, EntityStreamException  {
        try {
            return performExecute(queryExecutions);
        }
        catch (PreparingPersistStatementException x) {
            throw new BarleyDBQueryException("Error preparing query statement", x);
        }
    }

    private QueryEntityDataInputStream performExecute(QueryExecution<?>... queryExecutions) throws SortJdbcException, PreparingPersistStatementException, BarleyDBQueryException, EntityStreamException {
        if (queryExecutions == null || queryExecutions.length == 0) {
            throw new BarleyDBQueryException("No query executions...");
        }
        if (!database.supportsMultipleResultSets() || queryExecutions.length == 1) {
            entityContext.getStatistics().addNumberOfQueries(queryExecutions.length);
            entityContext.getStatistics().addNumberOfQueryDatabseCalls(queryExecutions.length);
            SeparateQueryResultManager resultManager = new SeparateQueryResultManager(queryExecutions);
            return new StreamingQueryExecutionProcessor( resultManager );
        }
        else {
            LOG.debug("Executing queries in one batch, processing multiple resultsets...");
            entityContext.getStatistics().addNumberOfQueries(queryExecutions.length);
            entityContext.getStatistics().addNumberOfQueryDatabseCalls(1);
            List<Param> params = new LinkedList<Param>();
            String sql = createCombinedQuery(params, queryExecutions);
            if (!params.isEmpty()) {
                //stmts cant close before rs
                try {
                    PreparedStatement stmt = prepareStatement(sql, runtimeProperties);

                    setFetch(stmt, runtimeProperties);

                    try {
                        //the first query execution will set all parameters
                        setParameters(stmt, params);
                        if (!stmt.execute()) {
                            throw new IllegalQueryStateException("Query did not return a result set");
                        }

                        CombinedQueryResultManager resultManager = new CombinedQueryResultManager(queryExecutions, stmt, stmt.getResultSet());
                        return new StreamingQueryExecutionProcessor(resultManager);
                    }
                    catch (SQLException x) {
                        throw new SortJdbcException("SQLException on prepared statement execute", x);
                    }
                }
                catch(SQLException x) {
                    throw new PreparingPersistStatementException("SQLException creating prepared statement", x);
                }
            }
            else {
                //stmts cant close before rs
                try {
                    Statement stmt = connection.createStatement();
                    try {
                        if (!stmt.execute(sql)) {
                            throw new IllegalQueryStateException("Query did not return a result set");
                        }
                    }
                    catch (SQLException x) {
                        throw new SortJdbcException("SQLException on statement execute", x);
                    }

                    CombinedQueryResultManager resultManager = new CombinedQueryResultManager(queryExecutions, stmt, stmt.getResultSet());
                    return new StreamingQueryExecutionProcessor(resultManager);
                }
                catch (SQLException x) {
                    throw new SortJdbcException("SQLException on statement create", x);
                }
            }
        }
    }

    /**
     * Executes a single query expecting one resultset.
     * @param queryExecution
     * @throws BarleyDBQueryException
     * @throws SQLException
     */
    private ResultSet executeQuery(QueryExecution<?> queryExecution) throws SortJdbcException, PreparingPersistStatementException, BarleyDBQueryException  {
        List<Param> params = new LinkedList<Param>();
        String sql = queryExecution.getSql(params);

        LOG.debug("============================================");
        LOG.debug("Executing individual query:\n" + sql);

        if (!params.isEmpty()) {
            try {
                PreparedStatement stmt = prepareStatement(sql, runtimeProperties);

                setFetch(stmt, runtimeProperties);

                setParameters(stmt, params);

                return stmt.executeQuery();
            }
            catch (SQLException x) {
                throw new PreparingPersistStatementException("SQLException preparing statement", x);
            }
        }
        else {
            try {
                Statement stmt = createStatement(runtimeProperties);

                setFetch(stmt, runtimeProperties);

                return stmt.executeQuery(sql);
            }
            catch (SQLException x) {
                throw new SortJdbcException("SQLException creating statement", x);
            }
        }
    }

    private void setFetch(Statement stmt, RuntimeProperties runtimeProperties) throws SortJdbcException {
        if (runtimeProperties != null && runtimeProperties.getFetchSize() != null) {
            try {
                stmt.setFetchSize( runtimeProperties.getFetchSize() );
                stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
            }
            catch (SQLException x) {
                throw new SortJdbcException("SQLException setting fetch size and direction", x);
            }
        }
    }

    private PreparedStatement prepareStatement(String sql, RuntimeProperties runtimeProperties) throws BarleyDBQueryException, SQLException {
        return connection.prepareStatement(sql, getResultSetType(runtimeProperties), getResultSetConcurrency(runtimeProperties));
    }

    private Statement createStatement(RuntimeProperties runtimeProperties) throws BarleyDBQueryException, SQLException {
        return connection.createStatement(getResultSetType(runtimeProperties), getResultSetConcurrency(runtimeProperties));
    }

    private static int getResultSetType(RuntimeProperties props) throws BarleyDBQueryException {
        if (props.getScrollType() == null) {
            return ResultSet.TYPE_FORWARD_ONLY;
        }
        switch(props.getScrollType()) {
            case FORWARD_ONLY:
                return ResultSet.TYPE_FORWARD_ONLY;
            case SCROLL_INSENSITIVE:
                return ResultSet.TYPE_SCROLL_INSENSITIVE;
            case SCROLL_SENSITIVE:
                return ResultSet.TYPE_SCROLL_SENSITIVE;
            default:
                throw new IllegalQueryStateException("Unknown scroll type '" + props.getScrollType() + "'");
        }
    }

    private static int getResultSetConcurrency(RuntimeProperties props) throws BarleyDBQueryException {
        if (props == null) {
            return ResultSet.CONCUR_READ_ONLY;
        }
        switch (props.getConcurrency()) {
            case READ_ONLY:
                return ResultSet.CONCUR_READ_ONLY;
            case UPDATABLE:
                return ResultSet.CONCUR_UPDATABLE;
            default:
                throw new IllegalQueryStateException("Unknown concurrency '" + props.getConcurrency() + "'");
        }
    }

    private void setParameters(PreparedStatement stmt, List<Param> params) throws BarleyDBQueryException  {
        int i = 1;
        QueryPreparedStatementHelper helper = new QueryPreparedStatementHelper(jdbcEntityContextServices, entityContext.getDefinitions());
        for (QueryGenerator.Param param : params) {
            helper.setParameter(stmt, i++, param.getNodeType(), param.getValue());
        }
    }

    private String createCombinedQuery(List<Param> params, QueryExecution<?>... queryExecutions) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        StringBuilder combinedQuery = new StringBuilder();

        for (QueryExecution<?> qExec : queryExecutions) {
            combinedQuery.append(' ');
            combinedQuery.append(qExec.getSql(params));
            combinedQuery.append("\n;");
        }
        combinedQuery.setLength(combinedQuery.length() - 1);
        String combinedQueryStr = combinedQuery.toString();
        LOG.debug("Created combined query:\n" + combinedQueryStr);
        return combinedQueryStr;
    }


    interface IResultManager {
        /**
         * The first query has index 0
         * @return
         */
        public int getQueryIndex();

        /**
        *
        * @return true if there is a next result
        * @throws SQLException
        */
        public boolean next() throws EntityStreamException;
        public ObjectGraph readObjectGraph() throws EntityStreamException;
        /**
         * Closes the data stream from which we get the results.
         * @throws EntityStreamException
         */
        public void close() throws EntityStreamException;
        public boolean hasNext();
    }

    private class SeparateQueryResultManager implements IResultManager {
        private QueryExecution<?> queryExecutions[];
        private Statement stmt;
        private ResultSet resultSet;
        private int queryIndex = 0;

        public SeparateQueryResultManager(QueryExecution<?> queryExecutions[]) {
            this.queryExecutions = queryExecutions;
        }

        public int getQueryIndex() {
            return queryIndex;
        }

        public boolean next() throws EntityStreamException {
            if (resultSet == null) {
                //check if we have any more query executions to perform.
                if (queryIndex >= queryExecutions.length) {
                    return false;
                }
                try {
                    resultSet = executeQuery( queryExecutions[ queryIndex ] );
                    stmt = resultSet.getStatement();
                    /*
                     * after we get the result set, we "fall through" and call resultSet.next so that
                     * the cursor is at the correct position.
                     */
                }
                catch (SQLException | SortJdbcException | BarleyDBQueryException  | PreparingPersistStatementException x) {
                    throw new EntityStreamException("Error executing query", x);
                }
            }
            try {
                if (resultSet.next()) {
                    return true;
                }
                //closes current resultset and statement
                closeCurrentResultSetAndStatement();
                queryIndex++;
                //try again
                return next();
            }
            catch (SQLException x) {
                throw new EntityStreamException("Error calling ResultSet.next", x);
            }
        }

        @Override
        public boolean hasNext() {
            return queryIndex < queryExecutions.length;
        }

        public ObjectGraph readObjectGraph() throws EntityStreamException {
            ObjectGraph og = new ObjectGraph();
            boolean moreData =  queryExecutions[ queryIndex ].readObjectGraph( resultSet, og, entityContext.getStatistics() );
            if (!moreData) {
                closeCurrentResultSetAndStatement();
                queryIndex++;
                next(); //try and move to the next query resultset (if there is one)
            }
            return og;
        }

        private void closeCurrentResultSetAndStatement() throws EntityStreamException {
            if (resultSet != null) {
                try {
                    LOG.trace("Closing result-set");
                    resultSet.close();
                }
                catch (SQLException x) {
                    try {
                        stmt.close();
                    }
                    catch(SQLException x2) {
                        x.addSuppressed(x2);
                    }
                    finally {
                        stmt = null;
                    }
                    throw new EntityStreamException("Error closing ResultSet", x);
                }
                finally {
                    resultSet = null;
                }
            }
            if (stmt != null) {
                try {
                    LOG.trace("Closing statement");
                    stmt.close();
                }
                catch(SQLException x) {
                    throw new EntityStreamException("Error closing Statement", x);
                }
                finally {
                    stmt = null;
                }
            }
        }

        public void close() throws EntityStreamException {
            EntityStreamException toThrow = null;
            try {
                closeCurrentResultSetAndStatement();
            }
            catch(EntityStreamException x) {
                toThrow = x;
            }
            if (closeConnection) {
                try {
                    connectionResources.close();
                }
                catch(SQLException x) {
                    if (toThrow == null) {
                        toThrow = new EntityStreamException("Could not close connection", x);
                    }
                    else {
                        toThrow.addSuppressed(x);
                    }
                }
            }
            if (toThrow != null) {
                throw toThrow;
            }
        }

    }

    private class CombinedQueryResultManager implements IResultManager {
        private final QueryExecution<?> queryExecutions[];
        private final Statement stmt;
        private ResultSet resultSet;
        private boolean finished;
        private int queryIndex = 0;

        public CombinedQueryResultManager(QueryExecution<?> queryExecutions[], Statement stmt, ResultSet resultSet) {
            this.queryExecutions = queryExecutions;
            this.stmt = stmt;
            this.resultSet = resultSet;
        }

        public int getQueryIndex() {
            return queryIndex;
        }

        public boolean next() throws EntityStreamException {
            try {
                if (resultSet != null && resultSet.next()) {
                    return true;
                }
            }
            catch (SQLException x) {
                throw new EntityStreamException("Error calling ResultSet.next", x);
            }
            try {
                if (stmt.getMoreResults()) {
                    //good, we have moved to the next result which is an honest-to-goodness ResultSet
                    resultSet = stmt.getResultSet();
                    queryIndex++;
                    //try again
                    return next();
                }
            }
            catch (Exception x) {
                throw new EntityStreamException("Error checking for or getting the next ResultSet", x);
            }
            //the stmt return false..., we have reach the end of result-data OR we have an update count
            resultSet = null;
            try {
                if (stmt.getUpdateCount() != -1) {
                    //means it WAS an update count, we may have more results yet.
                    //try again
                    return next();
                }
            }
            catch (SQLException x) {
                throw new EntityStreamException("Error getting the update count of the statement", x);
            }
            //the update count WAS -1, so no more result-sets and no more results.
            return !(finished = true);
        }

        @Override
        public boolean hasNext() {
            return finished;
        }

        public ObjectGraph readObjectGraph() throws EntityStreamException {
            ObjectGraph  objectGraph = new ObjectGraph();
            queryExecutions[ queryIndex ].readObjectGraph( resultSet, objectGraph, entityContext.getStatistics() );
            return objectGraph;
        }

        public void close() throws EntityStreamException {
            SQLException sqlx = null;
            try {
                LOG.debug("Closing result-set");
                resultSet.close();
            }
            catch (SQLException x) {
                sqlx = x;
            }
            try {
                LOG.debug("Closing statement");
                stmt.close();
            }
            catch(SQLException x) {
                if (sqlx == null) {
                    sqlx = x;
                }
                else {
                    sqlx.addSuppressed(x);
                }
            }
            if (closeConnection) {
                try {
                    connectionResources.close();
                }
                catch(SQLException x) {
                    if (sqlx == null) {
                        sqlx = x;
                    }
                    else {
                        sqlx.addSuppressed(x);
                    }
                }
            }
            if (sqlx != null) {
                throw new EntityStreamException("Could not close result manager resources", sqlx);
            }
        }

    }

}
