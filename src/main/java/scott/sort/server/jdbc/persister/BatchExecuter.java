package scott.sort.server.jdbc.persister;

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

    public void execute(Definitions definitions) throws Exception {
        if (group.getEntities().isEmpty()) {
            return;
        }
        PreparedStatementCache psCache = new PreparedStatementCache(definitions);
        PreparedStatement psLast = null;
        List<Entity> entities = new LinkedList<>();
        for (Entity entity : group.getEntities()) {
            PreparedStatement ps = prepareStatement(psCache, entity);
            if (psLast != null && psLast != ps) {
                executeBatch(psLast, entities);
                entities.clear();

            }
            ps.addBatch();
            entities.add(entity);
            psLast = ps;
        }
        executeBatch(psLast, entities);
        psCache.close();
    }

    private void executeBatch(PreparedStatement ps, List<Entity> entities) throws Exception {
        LOG.debug("Executing " + operationName + " batch for " + entities.get(0).getEntityType() + " of size " + entities.size());
        try {
            int counts[] = ps.executeBatch();
            if (counts.length != entities.size()) {
                throw new Exception("Not all entities were in the batch");
            }
            int totalMods = 0;
            for (int i = 0; i < counts.length; i++) {
                totalMods += counts[i];
                if (counts[i] == 0) {
                    handleFailure(entities.get(i), null);
                }
            }
            LOG.debug(totalMods + " rows were modified in total");
        } catch (BatchUpdateException x) {
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
    }

    protected abstract void handleFailure(Entity entity, Throwable throwable) throws Exception;

    protected abstract PreparedStatement prepareStatement(PreparedStatementCache psCache, Entity entity) throws SQLException;
}