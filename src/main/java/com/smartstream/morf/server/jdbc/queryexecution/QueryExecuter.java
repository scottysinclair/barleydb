package com.smartstream.morf.server.jdbc.queryexecution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.server.jdbc.queryexecution.QueryGenerator.Param;

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
    private final boolean supportsMultipleResultSets;

    public QueryExecuter(Connection connection, EntityContext entityContext) throws SQLException {
        this.connection = connection;
        this.entityContext = entityContext;
        if (connection.getMetaData().getDriverName().equals("HSQL Database Engine Driver")) {
            this.supportsMultipleResultSets = false;
        }
        else {
            this.supportsMultipleResultSets = connection.getMetaData().supportsMultipleResultSets();
        }
    }

    public void execute(QueryExecution<?>... queryExecutions) throws SQLException, Exception {
        entityContext.beginLoading();
        try {
            performExecute(queryExecutions);
        } finally {
            entityContext.endLoading();
        }
    }

    private void performExecute(QueryExecution<?>... queryExecutions) throws Exception {
        if (queryExecutions == null || queryExecutions.length == 0) {
            return;
        }
        if (!supportsMultipleResultSets || queryExecutions.length == 1) {
            for (QueryExecution<?> qExec : queryExecutions) {
                executeQuery(qExec);
            }
        }
        else {
            LOG.debug("Executing queries in one batch, processing multiple resultsets...");
            String sql = createCombinedQuery(queryExecutions);
            if (queryExecutions[0].hasParameters()) {
                try (PreparedStatement stmt = connection.prepareStatement( sql );) {
                    //the first query execution will set all parameters
                    queryExecutions[0].setParameters(stmt);
                    if (!stmt.execute()) {
                        throw new IllegalStateException("Query did not return a result set");
                    }
                    processResults(stmt, queryExecutions);
                }
            }
            else {
                try (Statement stmt = connection.createStatement();) {
                    if (!stmt.execute(sql)) {
                        throw new IllegalStateException("Query did not return a result set");
                    }
                    processResults(stmt, queryExecutions);
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

    private void processResults(Statement stmt, QueryExecution<?>... queryExecutions) throws Exception {
        int queryIndex = 0;
        int countResultSets = 0;
        boolean finished = false;
        do {
            try (ResultSet resultSet = stmt.getResultSet();) {
                queryExecutions[queryIndex++].processResultSet(resultSet);
            }
            countResultSets++;

            //There are no more results when the following is true:
            //((stmt.getMoreResults() == false) && (stmt.getUpdateCount() == -1))
            while (!finished && stmt.getMoreResults() == false) {
                if (stmt.getUpdateCount() == -1) {
                    finished = true;
                }
            }
        } while (!finished);
        if (countResultSets != queryExecutions.length) {
            throw new IllegalStateException("Executed " + queryExecutions.length + " queries but only received " + countResultSets + " resultsets");
        }
    }

    private void executeQuery(QueryExecution<?> queryExecution) throws SQLException {
        String sql = queryExecution.getSql();
        if (queryExecution.hasParameters()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql);) {
                LOG.debug("============================================");
                LOG.debug("Executing individual query:\n" + sql);
                queryExecution.setParameters(stmt);
                try (ResultSet resultSet = stmt.executeQuery()) {
                    queryExecution.processResultSet(resultSet);
                }
            }
        }
        else {
            try (Statement stmt = connection.createStatement();) {
                LOG.debug("============================================");
                LOG.debug("Executing individual query:\n" + sql);
                try (ResultSet resultSet = stmt.executeQuery(sql)) {
                    queryExecution.processResultSet(resultSet);
                }
            }
        }

    }

    private String createCombinedQuery(QueryExecution<?>... queryExecutions) {
        StringBuilder combinedQuery = new StringBuilder();
        List<Param> params = new LinkedList<Param>();
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
