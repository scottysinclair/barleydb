package scott.barleydb.server.jdbc.vendor;

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
