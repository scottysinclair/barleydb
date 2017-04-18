package scott.barleydb.server.jdbc.persist;

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

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.persist.EntityMissingException;
import scott.barleydb.api.exception.execution.persist.IllegalPersistStateException;
import scott.barleydb.api.exception.execution.persist.OptimisticLockMismatchException;
import scott.barleydb.api.exception.execution.persist.PreparingPersistStatementException;
import scott.barleydb.api.exception.execution.persist.PrimaryKeyExistsException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.specification.KeyGenSpec;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.persist.audit.AuditInformation;
import scott.barleydb.server.jdbc.persist.audit.AuditRecord;
import scott.barleydb.server.jdbc.persist.audit.Change;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.server.jdbc.vendor.Database;

public class Persister {

    private static final Logger LOG = LoggerFactory.getLogger(Persister.class);
    private static final Logger LOG_PERSIST_REPORT = LoggerFactory.getLogger(PersistAnalyser.class.getName() + ".report");
    private static final Logger AUDITLOG = LoggerFactory.getLogger(Persister.class.getName() + ".audit");

    private final Environment env;
    private final String namespace;
    private final JdbcEntityContextServices entityContextServices;

    public Persister(Environment env, String namespace, JdbcEntityContextServices entityContextServices) {
        this.env = env;
        this.namespace = namespace;
        this.entityContextServices = entityContextServices;
    }

    public AuditInformation compareWithDatabase(PersistAnalyser analyser) throws SortPersistException {
        DatabaseDataSet databaseDataSet = new DatabaseDataSet(analyser.getEntityContext());
        try {
            loadAndValidate(databaseDataSet, analyser.getUpdateGroup(), analyser.getDeleteGroup(), analyser.getDependsOnGroup());
        } catch (SortServiceProviderException x) {
            throw new SortPersistException("Error loading original data", x);
        }

        logStep("Generating audit logs");
        AuditInformation audit = new AuditInformation();
        audit.add(auditCreate(analyser.getCreateGroup()));
        audit.add(auditUpdate(databaseDataSet, analyser.getUpdateGroup()));
        audit.add(auditDelete(databaseDataSet, analyser.getDeleteGroup()));

        return audit;
    }

    public void persist(PersistAnalyser analyser) throws SortPersistException {
        if (analyser.getEntityContext().isUser()) {
            throw new IllegalPersistStateException("EntityContext must be set to internal.");
        }
        Database database = ConnectionResources.getMandatoryForPersist(analyser.getEntityContext()).getDatabase();
        DatabaseDataSet databaseDataSet = new DatabaseDataSet(analyser.getEntityContext());
        try {
            loadAndValidate(databaseDataSet, analyser.getUpdateGroup(), analyser.getDeleteGroup(), analyser.getDependsOnGroup());
        } catch (SortServiceProviderException x) {
            throw new SortPersistException("Error loading original data", x);
        }

        setPrimaryKeys(analyser.getCreateGroup());

        Long newOptimisticLockTime = System.currentTimeMillis();

        /*
         * Because the optimistic lock changes have NOT been applied to the entities yet, the
         * audit information will not contain entries for them yet.
         * This is good as we will use the audit information to find out what entities have changes
         * and therefore require sending to the database.
         *
         * The missing audit info will be added later
         */
        logStep("Generating audit logs");
        AuditInformation audit = new AuditInformation();
        audit.add(auditCreate(analyser.getCreateGroup()));
        audit.add(auditUpdate(databaseDataSet, analyser.getUpdateGroup()));
        audit.add(auditDelete(databaseDataSet, analyser.getDeleteGroup()));

        /*
         * analyse what entities require an update based on the audit information we just gathered
         * and also on the owning relationships.
         * ie a syntax OL should be updated if it's mapping changes since a syntax own's it's mapping and mappings
         * don't have optimistic locks.
         */
        Set<Entity> updateRequired = analyseRequiredUpdates(audit, analyser.getUpdateGroup());

        logStep("Filter out entities which won't change from the update batch group");
        filterOutUnchangedEntities(updateRequired, analyser.getUpdateGroup());

        if (LOG_PERSIST_REPORT.isDebugEnabled()) {
            LOG_PERSIST_REPORT.debug("Persist report after filtering...");
            LOG_PERSIST_REPORT.debug(analyser.report());
        }

        /*
         * Add audit records for the optimistic lock columns.
         * The real entities still contain the original OL values, we are keeping them for the where clauses
         * in the update and delete statements.
         * This is why we have to manually apply the OL audit information, it was not automatically detected.
         */
        /*
         * TODO:The optimistic lock should be type converted to the String timestamp for mysql
         */
        setNewOptimisticLockOnAuditRecords(audit, analyser.getCreateGroup(), analyser.getUpdateGroup(), newOptimisticLockTime);


        verifyAccessRights(analyser.getCreateGroup(), analyser.getUpdateGroup(), analyser.getDeleteGroup());

        /*
         * helpful for testing
         */
        preJdbcWorkHook();

        /*
         * We always insert before we update, in-case a pending update depends on a created record
         */
        try {
            insert(analyser.getCreateGroup(), newOptimisticLockTime, database);
        }
        catch (SortJdbcException x) {
            throw new SortPersistException("Error during insert", x);
        }

        /*
         * We always update before we delete, in-case a delete depends on a FK removal.
         */
        try {
            update(analyser.getUpdateGroup(), newOptimisticLockTime, database);
        }
        catch (SortJdbcException x) {
            throw new SortPersistException("Error during update", x);
        }

        try {
            delete(analyser.getDeleteGroup(), database);
        }
        catch (SortJdbcException x) {
            throw new SortPersistException("Error during delete", x);
        }

        insert(audit);

        /*
         * updates the optimistic lock nodes for all created and updated entities
         */
        updateOptimisticLocks(newOptimisticLockTime, analyser.getCreateGroup(), analyser.getUpdateGroup());

        /*
         * Clear out the deleted items
         */
        cleanDeletedItems(analyser.getEntityContext(), analyser.getDeleteGroup());

        /*
         * Clear added or deleted reference tracking
         */
        clearRefsForUpdatedEntities(analyser.getUpdateGroup());

        /*
         * All created entities should have the state loaded
         * and all of their ToMany refs should be fetched.
         */
        setLoadedAndFetchedForCreatedEntities(analyser.getCreateGroup());
    }

    protected void preJdbcWorkHook() {}

    private void logStep(String message) {
        LOG.debug("----------------------------------------------");
        LOG.debug(message + "...");
    }

    /**
     * Loads update and delete entities fresh from the data from the database.
     * Each entity must exist and have the correct optimistic lock.
     * @param databaseDataSet
     * @param updateGroup
     * @param deleteGroup
     * @param dependsOnGroup
     * @throws SortJdbcException
     * @throws SortPersistException
     */
    private void loadAndValidate(DatabaseDataSet databaseDataSet, OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws SortServiceProviderException, SortPersistException  {
        logStep("Loading dataset from database");
        try {
            databaseDataSet.loadEntities(updateGroup, deleteGroup, dependsOnGroup);
        }
        catch (SortQueryException x) {
            throw new SortPersistException("Could not load entities for validation and audit", x);
        }
        for (Entity entity : iterable(updateGroup, deleteGroup, dependsOnGroup)) {
            Entity databaseEntity = databaseDataSet.getEntity(entity.getEntityType(), entity.getKey().getValue());
            if (databaseEntity == null) {
                throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
            }
            verifyOptimisticLock(entity, databaseEntity);
        }
    }

    /**
     * Add audit records for create entities
     * We only audit changes, so from null to some value
     * if a node of the entity has a null value then we don't create a record for it
     * @param createGroup
     * @return
     * @throws IllegalPersistStateException
     */
    private List<AuditRecord> auditCreate(OperationGroup createGroup) throws IllegalPersistStateException {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : createGroup.getEntities()) {
            AuditRecord auditRecord = null;
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    if (((ValueNode) node).getValue() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, null, ((ValueNode) node).getValue());
                    }
                }
                else if (node instanceof RefNode) {
                    if (auditRecord == null) {//lazy init
                        auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                    }
                    if (((RefNode) node).getEntityKey() != null) {
                        auditRecord.addChange(node, null, ((RefNode) node).getEntityKey());
                    }
                }
            }
            if (auditRecord != null) {
                LOG.debug("Changes found for " + entity);
                records.add(auditRecord);
            }
        }
        return records;
    }

    /**
     * Audit entities which are updated
     * again only changes are audited
     * @param databaseDataSet
     * @param updateGroup
     * @return
     * @throws IllegalPersistStateException
     */
    private List<AuditRecord> auditUpdate(DatabaseDataSet databaseDataSet, OperationGroup updateGroup) throws IllegalPersistStateException {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : updateGroup.getEntities()) {
            AuditRecord auditRecord = null;
            Entity originalEntity = databaseDataSet.getEntity(entity.getEntityType(), entity.getKey().getValue());
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    ValueNode updatedNode = (ValueNode) node;
                    ValueNode origNode = originalEntity.getChild(node.getName(), ValueNode.class);
                    if (!Objects.equals(origNode.getValue(), updatedNode.getValue())) {
                        if (auditRecord == null) {
                            //lazy init of audit record
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, origNode.getValue(), updatedNode.getValue());
                    }
                }
                else if (node instanceof RefNode) {
                    RefNode origNode = originalEntity.getChild(node.getName(), RefNode.class);
                    RefNode updatedNode = (RefNode) node;
                    if (!Objects.equals(origNode.getEntityKey(), updatedNode.getEntityKey())) {
                        if (auditRecord == null) {
                            //lazy init of audit record
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, origNode.getEntityKey(), updatedNode.getEntityKey());
                    }
                }

            }
            if (auditRecord != null) {
                records.add(auditRecord);
                LOG.debug("Changes found for " + entity);
            }
            else {
                LOG.debug("No changes found for " + entity);
            }
        }
        return records;
    }

    /**
     * Only changes are audited, so fields which were null are not included
     * @param databaseDataSet
     * @param deleteGroup
     * @return
     * @throws IllegalPersistStateException
     */
    private List<AuditRecord> auditDelete(DatabaseDataSet databaseDataSet, OperationGroup deleteGroup) throws IllegalPersistStateException {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : deleteGroup.getEntities()) {
            AuditRecord auditRecord = null;
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    if (((ValueNode) node).getValue() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, ((ValueNode) node).getValue(), null);
                    }
                }
                else if (node instanceof RefNode) {
                    if (((RefNode) node).getEntityKey() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, ((RefNode) node).getEntityKey(), null);
                    }
                }
            }
            if (auditRecord != null) {
                LOG.debug("Changes found for " + entity);
                records.add(auditRecord);
            }
        }
        return records;
    }

    private void setPrimaryKeys(OperationGroup createGroup) throws SortPersistException {
        logStep("Setting primary keys");
        for (Entity entity : createGroup.getEntities()) {
            if (entity.getKey().getValue() == null && entity.getEntityType().getKeyGenSpec() == KeyGenSpec.FRAMEWORK) {
                Object value = entityContextServices.getSequenceGenerator().getNextKey(entity.getEntityType());
                LOG.debug("Setting key for " + entity + " to " + value);
                entity.getKey().setValue(value);
            }
        }
    }

    /**
     * calculates the set of entities which require their OL to be updated
     * @param audit
     * @param updateGroup
     * @return
     */
    private Set<Entity> analyseRequiredUpdates(AuditInformation audit, OperationGroup updateGroup) {
        Set<Entity> updateRequired = new HashSet<>();
        Set<Entity> updateNotRequired = new HashSet<>();
        for (Entity entity : updateGroup.getEntities()) {
            analyseEntityRequiresUpdate(audit, entity, updateRequired, updateNotRequired);
        }
        return updateRequired;
    }

    /**
     * analyses the entity and it's dependents adding it and them to the relevant sets
     * @param audit
     * @param entity
     * @param updateRequired
     * @param updateNotRequired
     */
    private boolean analyseEntityRequiresUpdate(AuditInformation audit, Entity entity, Set<Entity> updateRequired, Set<Entity> updateNotRequired) {
        if (updateRequired.contains(entity)) {
            return true;
        }
        if (updateNotRequired.contains(entity)) {
            return false;
        }
        //we have an audit record for this entity, it is being directly modified, so tag it as required
        if (audit.contains(entity)) {
            updateRequired.add(entity);
            return true;
        }
        /*
         *loop through all refnodes nodes to check if they cause us to require modification.
         */
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            //an added entity ref would be detected in the audit information since this directly affects a property of the entity
            //a removed entity ref would be detected in the audit information since this directly affects a property of the entity

            //the reference is null and was always null otherwise the audit check above would catch the change
            if (refNode.getReference() == null) {
                continue;
            }
            //if it has not been loaded from the db (and therefore not modified) then there is no update required.
            if (refNode.getReference().isFetchRequired()) {
                continue;
            }
            //we don't own the node, so no changes reflect on us
            if (!refNode.getNodeType().isOwns()) {
                continue;
            }
            //the referred to entity type has it's own optimistic lock, so we don't need to touch ours
            if (refNode.getEntityType().supportsOptimisticLocking()) {
                continue;
            }
            //if we get so far then we own an entity which doesn't have it's own optimistic lock
            //check if it requires an update
            boolean ownedEntityRequiresUpdate = analyseEntityRequiresUpdate(audit, refNode.getReference(), updateRequired, updateNotRequired);
            if (ownedEntityRequiresUpdate) {
                LOG.debug("Update required to optimistic lock for " + entity + " due to ref to " + refNode.getReference());
                updateRequired.add(entity);
                return true;
            }
        }
        /*
         *loop through all tomany nodes to check if they cause us to require modification.
         */
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            //the to many relation was never fetched, so no changes occurred.
            if (!toManyNode.isFetched()) {
                continue;
            }
            //we don't own the node, so no changes reflect on us
            if (!toManyNode.getNodeType().isOwns()) {
                continue;
            }
            //the entity has it's own optimistic lock so we don't need to touch ours
            if (toManyNode.getEntityType().supportsOptimisticLocking()) {
                continue;
            }
            //check if any of our tomany entities have been modified
            for (Entity toManyEntity : toManyNode.getList()) {
                boolean ownedEntityRequiresUpdate = false;
                //the tomany entity which we own was not loaded and is therefore new
                //meaning we need to update.
                if (toManyEntity.isClearlyNotInDatabase()) {
                    ownedEntityRequiresUpdate = true;
                }
                else {
                    ownedEntityRequiresUpdate = analyseEntityRequiresUpdate(audit, toManyEntity, updateRequired, updateNotRequired);
                }
                if (ownedEntityRequiresUpdate) {
                    LOG.debug("Update required to optimistic lock for " + entity + " due to ref to " + toManyEntity);
                    updateRequired.add(entity);
                    return true;
                }
            }
        }
        /*
         * got to the end, so no update required
         */
        updateNotRequired.add(entity);
        return false;
    }

    private void setNewOptimisticLockOnAuditRecords(AuditInformation audit, OperationGroup createGroup, OperationGroup updateGroup, Long newOptimisticLockTime) {
        for (Entity entity : createGroup.mergedCopy(updateGroup).getEntities()) {
            if (entity.getEntityType().supportsOptimisticLocking()) {
                AuditRecord auditRecord = audit.getOrCreateRecord(entity);
                if (auditRecord != null) {
                    auditRecord.setOptimisticLock(entity, newOptimisticLockTime);
                }
            }
        }
    }

    /**
     * Sets the optimistic lock values on the entities.
     * @param optimisticLockTime
     * @param createGroup
     * @param updateGroup
     */
    private void updateOptimisticLocks(Long optimisticLockTime, OperationGroup createGroup, OperationGroup updateGroup) {
        logStep("Updating optimistic locks");
        updateOptimisticLocks(optimisticLockTime, createGroup.getEntities());
        updateOptimisticLocks(optimisticLockTime, updateGroup.getEntities());
    }

    /**
     * Sets the optimistic lock values on the entities.
     * @param optimisticLockTime
     * @param entities
     */
    private void updateOptimisticLocks(Long optimisticLockTime, List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.getEntityType().supportsOptimisticLocking()) {
                entity.getOptimisticLock().setValue(optimisticLockTime);
            }
        }
    }

    /**
     * Removes entities from the updateGroup which are not in the updateRequired set.
     * @param updateRequired
     * @param updateGroup
     */
    private void filterOutUnchangedEntities(Set<Entity> updateRequired, OperationGroup updateGroup) {
        for (Iterator<Entity> it = updateGroup.getEntities().iterator(); it.hasNext();) {
            Entity entity = it.next();
            if (!updateRequired.contains(entity)) {
                it.remove();
                LOG.debug("Filtered out " + entity);
            }
        }
    }

    /**
     * Verifies the access rights for the operation groups.
     * @param createGroup
     * @param updateGroup
     * @param deleteGroup
     */
    private void verifyAccessRights(OperationGroup createGroup, OperationGroup updateGroup, OperationGroup deleteGroup) {
        logStep("Verifying access rights");
        for (Entity entity : createGroup.getEntities()) {
            verifyCreateRight(entity);
        }
        for (Entity entity : updateGroup.getEntities()) {
            verifyUpdateRight(entity);
        }
        for (Entity entity : deleteGroup.getEntities()) {
            verifyDeleteRight(entity);
        }
    }

    private void insert(OperationGroup createGroup, final Long optimisticLockTime, final Database database) throws SortPersistException, SortJdbcException  {
        logStep("Performing inserts");
        BatchExecuter batchExecuter = new BatchExecuter(createGroup, "insert", database) {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementPersistCache psCache, Entity entity) throws SortPersistException {
                return psCache.prepareInsertStatement(entity, optimisticLockTime);
            }

            @Override
            protected void handleNoop(Entity entity, Throwable throwable) throws SortPersistException {
                throw new IllegalPersistStateException("No update count from insert operation for entity " + entity);
            }

            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws SortPersistException {
                handleInsertFailure(entity, throwable);
            }
        };
        batchExecuter.execute(entityContextServices, env.getDefinitions(namespace));
    }

    private void update(OperationGroup updateGroup, final Long newOptimisticLockTime, final Database database) throws PreparingPersistStatementException, SortJdbcException, SortPersistException {
        logStep("Performing updates");
        BatchExecuter batchExecuter = new BatchExecuter(updateGroup, "update", database) {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementPersistCache psCache, Entity entity) throws SortPersistException {
                return psCache.prepareUpdateStatement(entity, newOptimisticLockTime);
            }
            @Override
            protected void handleNoop(Entity entity, Throwable throwable) throws SortPersistException {
                handleUpdateNoop(entity);
            }
            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws SortPersistException {
                handleUpdateFailure(entity, throwable);
            }
        };
        batchExecuter.execute(entityContextServices, env.getDefinitions(namespace));
    }

    private void delete(OperationGroup deleteGroup, final Database database) throws PreparingPersistStatementException, SortPersistException, SortJdbcException {
        logStep("Performing deletes");
        BatchExecuter batchExecuter = new BatchExecuter(deleteGroup, "delete", database) {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementPersistCache psCache, Entity entity) throws SortPersistException {
                return psCache.prepareDeleteStatement(entity);
            }
            @Override
            protected void handleNoop(Entity entity, Throwable throwable) throws SortPersistException {
                handleDeleteNoop(entity);
            }
            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws SortPersistException {
                handleDeleteFailure(entity, throwable);
            }
        };
        batchExecuter.execute(entityContextServices, env.getDefinitions(namespace));
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyCreateRight(Entity entity) {
        LOG.debug("VERIFYING CREATE RIGHT FOR " + entity);
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyUpdateRight(Entity entity) {
        LOG.debug("VERIFYING UPDATE RIGHT FOR " + entity);
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyDeleteRight(Entity entity) {
        LOG.debug("VERIFYING DELETE RIGHT FOR " + entity);
    }

    private void insert(AuditInformation audit) {
        logStep("Inserting audit records");
        for (AuditRecord auditRecord : audit.getRecords()) {
            for (Change change : auditRecord.changes()) {
                AUDITLOG.debug( auditRecord.formatChange(change) );
            }
        }
    }

    private void verifyOptimisticLock(Entity entity, Entity databaseEntity) throws OptimisticLockMismatchException {
        if (entity.getEntityType().supportsOptimisticLocking()) {
            if (entity.compareOptimisticLocks(databaseEntity) != 0) {
                LOG.debug("Optimistic lock mismatch: ours={" + entity + "," + entity.getOptimisticLock().getValue() + "}, database={" + databaseEntity + "," + databaseEntity.getOptimisticLock().getValue() + "}");
                throw new OptimisticLockMismatchException(entity, databaseEntity);
            }
            LOG.debug("Optimistic lock verified: " + entity + " " + entity.getOptimisticLock().getValue());
        }
    }

    /**
     * Created objects are now in the database
     * So their to many are all considered fetched
     * If there are to-many references then they were also inserted
     * IF there are not to-many references then they were not inserted
     * either way the entity state matches the DB and a fetch is not required
     * @param group
     */
    private void setLoadedAndFetchedForCreatedEntities(OperationGroup createGroup) {
        logStep("Setting created entities to loaded and fetched");

        /*
         * Set all to state loaded and fetched
         */
        for (Entity en : createGroup.getEntities()) {
            en.getConstraints().setMustExistInDatabase();
            en.setEntityState(EntityState.LOADED);
            for (ToManyNode toManyNode : en.getChildren(ToManyNode.class)) {
                toManyNode.setFetched(true);
            }
        }
        /*
         * Then clear their state to normal (no new or removed entities as they are now saved).
         */
        for (Entity en : createGroup.getEntities()) {
            //clear our ref states back to normal, the updated or deleted refs will be removed
            en.clear();
        }
    }

    /**
     * The RefNode updated state can be cleared
     * The ToMany node updated state can be clear
     *
     * This should be done after the deleted entity keys are set to null
     * so that the ToMany refresh will remove them and collect the right ones
     *
     * @param updateGroup
     */
    private void clearRefsForUpdatedEntities(OperationGroup updateGroup) {
        logStep("updating references for updated entities");
        for (Entity en : updateGroup.getEntities()) {
            en.clear();
        }
    }

    private void cleanDeletedItems(EntityContext entityContext, OperationGroup deleteGroup) {
        logStep("setting deleted entity keys to null and state NEW");
        for (Entity entity : deleteGroup.getEntities()) {
            //TODO:only set to null if key is auto generated.
            //no longer setting it to null seems to have no impact
//            entity.getKey().setValue(null);
            entity.setEntityState(EntityState.NOT_IN_DB);
            entity.getConstraints().setMustNotExistInDatabase();
        }
    }

    private Iterable<Entity> iterable(OperationGroup... groups) {
        List<Entity> list = new LinkedList<>();
        for (OperationGroup og : groups) {
            list.addAll(og.getEntities());
        }
        return list;
    }

    private void handleInsertFailure(Entity entity, Throwable throwable) throws SortPersistException {
        EntityContext tempCtx = entity.getEntityContext().newEntityContextSharingTransaction();
        Entity loadedEntity = tempCtx.getEntityOrLoadEntity(entity.getEntityType(), entity.getKey().getValue(), false);
        if (loadedEntity != null) {
            throw new PrimaryKeyExistsException(entity.getEntityType(), entity.getKey().getValue());
        }
        else {
            throw new SortPersistException("Could not insert entity: " + entity, throwable);
        }
    }

    /**
     * Check if the entity has been deleted from the database preventing our update.
     * Check for optimistic lock violation.
     * @param entity
     * @param throwable
     * @throws SortPersistException
     */
    private void handleUpdateFailure(Entity entity, Throwable throwable) throws SortPersistException {
        EntityContext tempCtx = entity.getEntityContext().newEntityContextSharingTransaction();
        Entity loadedEntity = tempCtx.getEntityOrLoadEntity(entity.getEntityType(), entity.getKey().getValue(), false);
        if (loadedEntity == null) {
            throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
        }
        else if (loadedEntity.getOptimisticLock() != null && !Objects.equals(loadedEntity.getOptimisticLock().getValue(), entity.getOptimisticLock().getValue())) {
            throw new OptimisticLockMismatchException(entity, loadedEntity);
        }
        else {
            throw new SortPersistException("Could not update entity: " + entity, throwable);
        }
    }

    private void handleUpdateNoop(Entity entity) throws SortPersistException {
        LOG.debug("Analysing update noop, querying for problematic entity {}", entity);
        /*
         * The tempCtx takes a fresh entity context with it's own transaction.
         * This guarantees absolute freshness when querying for the (most likely deleted entity).
         *
         * For example MySql's repeatable read transactions, are based on a MVCC system
         * And the transaction's database state is fixed from the first query in that transaction.
         *
         */
        //EntityContext tempCtx = new EntityContext(env, entity.getEntityContext().getNamespace());
        EntityContext tempCtx = entity.getEntityContext().newEntityContextSharingTransaction();
        Entity loadedEntity = tempCtx.getEntityOrLoadEntity(entity.getEntityType(), entity.getKey().getValue(), false);
        if (loadedEntity == null) {
            throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
        }
        else if (!Objects.equals(loadedEntity.getOptimisticLock().getValue(), entity.getOptimisticLock().getValue())) {
            throw new OptimisticLockMismatchException(entity, loadedEntity);
        }
        else {
            throw new SortPersistException("Update was a noop for an unknown reason: "  + entity);
        }
    }

    private void handleDeleteNoop(Entity entity) throws SortPersistException {
        /*
         * The tempCtx takes a fresh entity context with it's own transaction.
         * This guarantees absolute freshness when querying for the (most likely deleted entity).
         *
         * For example MySql's repeatable read transactions, are based on a MVCC system
         * And the transaction's database state is fixed from the first query in that transaction.
         *
         */
        //EntityContext tempCtx = new EntityContext(env, entity.getEntityContext().getNamespace());
        EntityContext tempCtx = entity.getEntityContext().newEntityContextSharingTransaction();
        Entity loadedEntity = tempCtx.getEntityOrLoadEntity(entity.getEntityType(), entity.getKey().getValue(), false);
        if (loadedEntity == null) {
            throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
        }
        else if (!Objects.equals(loadedEntity.getOptimisticLock().getValue(), entity.getOptimisticLock().getValue())) {
            throw new OptimisticLockMismatchException(entity, loadedEntity);
        }
        else {
            throw new SortPersistException("Delete was a noop for an unknown reason: "  + entity);
        }
    }

    private void handleDeleteFailure(Entity entity, Throwable throwable) throws SortPersistException {
        handleUpdateFailure(entity, throwable);
    }

}
