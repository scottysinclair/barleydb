package scott.sort.server.jdbc.database;

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

/**
 * The abstract interface for working with a specific database.
 *
 * We can ask questions about the capabilities and ask
 * it to render SQL for us.
 *
 * @author scott
 *
 */
public interface Database {

    String getInfo();

    boolean matches(DatabaseMetaData metaData) throws SQLException;

    /**
     * True when compound queries can be sent to the database
     * and multiple results can come back
     * @return
     */
    boolean supportsMultipleResultSets();

    /**
     * True if the database will return the proper update counts
     * when performing batch operations
     *
     * If propert batch update counts are not given then SUCCESS_NO_INFO is
     * returned by the JDBC driver instead.
     *
     * @return
     */
    boolean supportsBatchUpdateCounts();


    /**
     * True if the database will set all of the update counts to EXECUTE_FAILED in the batch
     * even if just one operation failed.
     *
     * Oracle does this.
     *
     * @return
     */
    boolean flagsAllOperationsAsFailedOnBatchUpdateException();

    /**
     * Supports the select ... for update statement
     * @return
     */
    boolean supportsSelectForUpdate();

    /**
     * Supports the select ... for update wait n statement.
     * @return
     */
    boolean supportsSelectForUpdateWaitN();

}
