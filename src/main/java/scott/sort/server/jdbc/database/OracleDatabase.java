package scott.sort.server.jdbc.database;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class OracleDatabase implements Database {

    private String info;

    public OracleDatabase(DatabaseMetaData metaData) throws SQLException {
        info = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public boolean matches(DatabaseMetaData metaData) throws SQLException {
        return "oracle".equalsIgnoreCase(metaData.getDatabaseProductName());
    }

    @Override
    public boolean supportsMultipleResultSets() {
        return false;
    }

    @Override
    public boolean supportsBatchUpdateCounts() {
        return false;
    }

    @Override
    public boolean flagsAllOperationsAsFailedOnBatchUpdateException() {
        return true;
    }

    @Override
    public boolean supportsSelectForUpdate() {
        return true;
    }

    @Override
    public boolean supportsSelectForUpdateWaitN() {
        return true;
    }


}
