package scott.sort.server.jdbc.database;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class SqlServerDatabase implements Database {

    private final boolean supportsMultipleResultSets;
    private final boolean supportsSelectForUpdate;

    @Override
    public boolean matches(DatabaseMetaData metaData) throws SQLException {
        return "Microsoft SQL Server".equals(metaData.getDatabaseProductName()) &&
                metaData.getDatabaseProductVersion().compareTo("11.00.2100") >= 0;
    }


    public SqlServerDatabase(DatabaseMetaData metaData) throws SQLException {
        supportsMultipleResultSets = metaData.supportsMultipleResultSets();
        supportsSelectForUpdate = metaData.supportsSelectForUpdate();
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
