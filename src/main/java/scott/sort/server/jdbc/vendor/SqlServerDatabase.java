package scott.sort.server.jdbc.vendor;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class SqlServerDatabase implements Database {

    private String info;
    private final boolean supportsMultipleResultSets;
    private final boolean supportsSelectForUpdate;

    public SqlServerDatabase(DatabaseMetaData metaData) throws SQLException {
        info = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
        supportsMultipleResultSets = metaData.supportsMultipleResultSets();
        supportsSelectForUpdate = metaData.supportsSelectForUpdate();
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public boolean matches(DatabaseMetaData metaData) throws SQLException {
        return "Microsoft SQL Server".equals(metaData.getDatabaseProductName()) &&
                metaData.getDatabaseProductVersion().compareTo("11.00.2100") >= 0;
    }

    @Override
    public boolean supportsMultipleResultSets() {
        return supportsMultipleResultSets;
    }

    @Override
    public boolean supportsBatchUpdateCounts() {
        return true;
    }

    @Override
    public boolean flagsAllOperationsAsFailedOnBatchUpdateException() {
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() {
        return supportsSelectForUpdate;
    }

    @Override
    public boolean supportsSelectForUpdateWaitN() {
        return false;
    }

}
