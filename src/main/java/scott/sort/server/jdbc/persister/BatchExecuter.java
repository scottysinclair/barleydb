package scott.sort.server.jdbc.persister;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.exception.AddBatchException;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.persist.PreparingPersistStatementException;
import scott.sort.server.jdbc.persister.exception.SortPersistException;

/**
 * Executes batch operations on a set of entities across various tables.
 * Contiguous entities of the same type will participate together in a JDBC batch operation.
 * @author scott
 *
 */
abstract class BatchExecuter {

    private static final Logger LOG = LoggerFactory.getLogger(BatchExecuter.class);

    private final OperationGroup group;
    private final String operationName;

    public BatchExecuter(OperationGroup group, String operationName) {
        this.group = group;
        this.operationName = operationName;
    }

    public void execute(Definitions definitions) throws PreparingPersistStatementException, SortPersistException, SortJdbcException {
        if (group.getEntities().isEmpty()) {
            return;
        }
        try ( PreparedStatementPersistCache psCache = new PreparedStatementPersistCache(definitions);) {
            PreparedStatement psLast = null;
            List<Entity> entities = new LinkedList<>();
            for (Entity entity : group.getEntities()) {
                PreparedStatement ps = prepareStatement(psCache, entity);
                if (psLast != null && psLast != ps) {
                    executeBatch(psLast, entities);
                    entities.clear();

                }
                try {
                    ps.addBatch();
                }
                catch(SQLException x) {
                    throw new AddBatchException("SQLException adding batch", x);

                }
                entities.add(entity);
                psLast = ps;
            }
            executeBatch(psLast, entities);
        }
    }

    private void executeBatch(PreparedStatement ps, List<Entity> entities) throws SortPersistException  {
        final String contextInfo = "executing " + operationName + " batch for " + entities.get(0).getEntityType() + " of size " + entities.size();
        LOG.debug(contextInfo);
        try {
            int counts[] = ps.executeBatch();
            if (counts.length != entities.size()) {
                throw new SortPersistException("The number update counts returned, does not match the size of the batch counts=" + counts.length + ", entities=" + entities.size() );
            }
            int totalMods = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == Statement.SUCCESS_NO_INFO) {

                }
                else if (counts[i] == Statement.EXECUTE_FAILED) {
                    /*
                     * Indicates that an operation failed.
                     * The driver only gives this if the database continues processing
                     * the other commands in the batch
                     */
                    handleFailure(entities.get(i), null);
                }
                totalMods += counts[i];
                if (counts[i] == 0) {
                    handleFailure(entities.get(i), null);
                }
            }
            LOG.debug(totalMods + " rows were modified in total");
        }
        catch (BatchUpdateException x) {
            int counts[] = x.getUpdateCounts();
            if (counts.length < entities.size()) {
                /*
                 * the counts are less then the batch size
                 * so the counts were the successfull ones
                 */
                for (int i = counts.length, n = entities.size(); i < n; i++) {
                    handleFailure(entities.get(i), x);
                }
            }
            else {
                /*
                 * all rows were processed, and we have to check the count status
                 */
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] == Statement.EXECUTE_FAILED) {
                        handleFailure(entities.get(i), x);
                    }

                }
            }
        }
        catch(SQLException x) {
            //thrown by the executeBatch call for a generic problem
            throw new SortPersistException("SQLException when " + contextInfo, x);
        }

    }

    protected abstract void handleFailure(Entity entity, Throwable throwable) throws SortPersistException;

    protected abstract PreparedStatement prepareStatement(PreparedStatementPersistCache psCache, Entity entity) throws SortPersistException;
}