package scott.sort.server.jdbc.queryexecution;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.persist.PreparingPersistStatementException;
import scott.sort.api.exception.query.ForUpdateNotSupportedException;
import scott.sort.api.exception.query.IllegalQueryStateException;
import scott.sort.api.exception.query.PreparingQueryStatementException;
import scott.sort.api.exception.query.SortQueryException;
import scott.sort.api.query.RuntimeProperties;
import scott.sort.server.jdbc.database.Database;
import scott.sort.server.jdbc.queryexecution.QueryGenerator.Param;

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

    private final Connection connection;
    private final EntityContext entityContext;
    private final Database database;
    private final RuntimeProperties runtimeProperties;

    public QueryExecuter(Database database, Connection connection, EntityContext entityContext, RuntimeProperties runtimeProperties)  {
        this.database = database;
        this.connection = connection;
        this.entityContext = entityContext;
        this.runtimeProperties = runtimeProperties;
    }

    /**
     * Executes the given query executions#
     *
     * If possible, in one database roundtrip.
     * @param queryExecutions
     * @throws PreparingPersistStatementException
     * @throws SortQueryException
     * @throws SQLException
     * @throws Exception
     */
    public void execute(QueryExecution<?>... queryExecutions) throws SortJdbcException, SortQueryException  {
        entityContext.beginLoading();
        try {
            performExecute(queryExecutions);
        }
        catch (PreparingPersistStatementException x) {
            throw new SortQueryException("Error preparing query statement", x);
        }
        finally {
            entityContext.endLoading();
        }
    }

    private void performExecute(QueryExecution<?>... queryExecutions) throws SortJdbcException, PreparingPersistStatementException, SortQueryException {
        if (queryExecutions == null || queryExecutions.length == 0) {
            return;
        }
        if (!database.supportsMultipleResultSets() || queryExecutions.length == 1) {
            for (QueryExecution<?> qExec : queryExecutions) {
                executeQuery(qExec);
            }
        }
        else {
            LOG.debug("Executing queries in one batch, processing multiple resultsets...");
            List<Param> params = new LinkedList<Param>();
            String sql = createCombinedQuery(params, queryExecutions);
            if (!params.isEmpty()) {
                try (PreparedStatement stmt = prepareStatement(sql, runtimeProperties)) {

                    setFetch(stmt, runtimeProperties);

                    try {
                        //the first query execution will set all parameters
                        setParameters(stmt, params);
                        if (!stmt.execute()) {
                            throw new IllegalQueryStateException("Query did not return a result set");
                        }
                        processResults(stmt, queryExecutions);
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
                try (Statement stmt = connection.createStatement();) {
                    try {
                        if (!stmt.execute(sql)) {
                            throw new IllegalQueryStateException("Query did not return a result set");
                        }
                    }
                    catch (SQLException x) {
                        throw new SortJdbcException("SQLException on statement execute", x);
                    }
                    processResults(stmt, queryExecutions);
                }
                catch (SQLException x) {
                    throw new SortJdbcException("SQLException on statement create", x);
                }
            }
        }
        /*
        * Tell each query excecution to finish
        */
        for (QueryExecution<?> queryExecution : queryExecutions) {
            queryExecution.finish();
        }
    }

    private void processResults(Statement stmt, QueryExecution<?>... queryExecutions) throws SortJdbcException, SortQueryException  {
        int queryIndex = 0;
        int countResultSets = 0;
        boolean finished = false;
        do {
            try (ResultSet resultSet = stmt.getResultSet();) {
                queryExecutions[queryIndex++].processResultSet(resultSet);
            }
            catch (SQLException x) {
                throw new SortJdbcException("SQLException getting the statement result", x);
            }
            countResultSets++;

            //There are no more results when the following is true:
            //((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
            try {
                while (!finished && stmt.getMoreResults() == false) {
                    try {
                        if (stmt.getUpdateCount() == -1) {
                            finished = true;
                        }
                    }
                    catch (SQLException x) {
                        throw new SortJdbcException("SQLException calling getUpdateCount on the statement", x);
                    }
                }
            }
            catch (SQLException x) {
                throw new SortJdbcException("SQLException calling getMoreResults on the statement", x);
            }
        } while (!finished);
        if (countResultSets != queryExecutions.length) {
            throw new IllegalQueryStateException("Executed " + queryExecutions.length + " queries but only received " + countResultSets + " resultsets");
        }
    }

    /**
     * Executes a single query expecting one resultset.
     * @param queryExecution
     * @throws SortQueryException
     * @throws SQLException
     */
    private void executeQuery(QueryExecution<?> queryExecution) throws SortJdbcException, PreparingPersistStatementException, SortQueryException  {
        List<Param> params = new LinkedList<Param>();
        String sql = queryExecution.getSql(params);
        LOG.debug("============================================");
        LOG.debug("Executing individual query:\n" + sql);

        if (!params.isEmpty()) {
            try (PreparedStatement stmt = prepareStatement(sql, runtimeProperties)) {

                setFetch(stmt, runtimeProperties);

                setParameters(stmt, params);
                try (ResultSet resultSet = stmt.executeQuery()) {
                    queryExecution.processResultSet(resultSet);
                }
                catch (SQLException x) {
                    throw new SortJdbcException("SQLException executing the prepared statement query", x);
                }
            }
            catch (SQLException x) {
                throw new PreparingPersistStatementException("SQLException preparing statement", x);
            }
        }
        else {
            try (Statement stmt = createStatement(runtimeProperties)) {

                setFetch(stmt, runtimeProperties);

                LOG.debug("============================================");
                LOG.debug("Executing individual query:\n" + sql);
                try (ResultSet resultSet = stmt.executeQuery(sql)) {
                    queryExecution.processResultSet(resultSet);
                }
                catch (SQLException x) {
                    throw new SortJdbcException("SQLException executing the statement query", x);
                }
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

    private PreparedStatement prepareStatement(String sql, RuntimeProperties runtimeProperties) throws SortQueryException, SQLException {
        return connection.prepareStatement(sql, getResultSetType(runtimeProperties), getResultSetConcurrency(runtimeProperties));
    }

    private Statement createStatement(RuntimeProperties runtimeProperties) throws SortQueryException, SQLException {
        return connection.createStatement(getResultSetType(runtimeProperties), getResultSetConcurrency(runtimeProperties));
    }

    private static int getResultSetType(RuntimeProperties props) throws SortQueryException {
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

    private static int getResultSetConcurrency(RuntimeProperties props) throws SortQueryException {
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

    private void setParameters(PreparedStatement stmt, List<Param> params) throws PreparingQueryStatementException  {
        int i = 1;
        QueryPreparedStatementHelper helper = new QueryPreparedStatementHelper(entityContext.getDefinitions());
        for (QueryGenerator.Param param : params) {
            helper.setParameter(stmt, i++, param.getNodeDefinition(), param.getValue());
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
}
