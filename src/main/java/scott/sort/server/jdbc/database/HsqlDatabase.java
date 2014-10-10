package scott.sort.server.jdbc.database;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class HsqlDatabase implements Database {

    private String info;

    public HsqlDatabase(DatabaseMetaData metaData) throws SQLException {
        info = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public boolean matches(DatabaseMetaData metaData) throws SQLException {
        return metaData.getDatabaseProductName().equals("HSQL Database Engine") &&
                metaData.getDatabaseProductVersion().compareTo("2.3.2") >= 0;
    }

    @Override
    public boolean supportsMultipleResultSets() {
        return false;
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
        return false;
    }

    @Override
    public boolean supportsSelectForUpdateWaitN() {
        return false;
    }

}
