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
import scott.sort.api.exception.query.IllegalQueryStateException;
import scott.sort.api.exception.query.PreparingQueryStatementException;
import scott.sort.api.exception.query.SortQueryException;
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

    public QueryExecuter(Database database, Connection connection, EntityContext entityContext)  {
        this.database = database;
        this.connection = connection;
        this.entityContext = entityContext;
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
                try (PreparedStatement stmt = connection.prepareStatement(sql);) {
                    try {
                        //the first query execution will set all parameters
                        setParameters(stmt, params);
                        if (!stmt.execute()) {
                            throw new IllegalStateException("Query did not return a result set");
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
        if (!params.isEmpty()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql);) {
                LOG.debug("============================================");
                LOG.debug("Executing individual query:\n" + sql);
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
            try (Statement stmt = connection.createStatement();) {
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

    private void setParameters(PreparedStatement stmt, List<Param> params) throws PreparingQueryStatementException  {
        int i = 1;
        QueryPreparedStatementHelper helper = new QueryPreparedStatementHelper(entityContext.getDefinitions());
        for (QueryGenerator.Param param : params) {
            helper.setParameter(stmt, i++, param.getNodeDefinition(), param.getValue());
        }
    }

    private String createCombinedQuery(List<Param> params, QueryExecution<?>... queryExecutions) throws IllegalQueryStateException {
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
