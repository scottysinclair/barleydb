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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.*;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;

public class OperationGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(OperationGroup.class);

    private final List<Entity> entities = new LinkedList<>();

    public OperationGroup reverse() {
        OperationGroup og = new OperationGroup();
        og.entities.addAll(entities);
        Collections.reverse(og.entities);
        return og;
    }

    public void add(Entity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public OperationGroup mergedCopy(OperationGroup... ogs) {
        OperationGroup ogm = new OperationGroup();
        ogm.entities.addAll(entities);
        for (OperationGroup og : ogs) {
            ogm.entities.addAll(og.entities);
        }
        return ogm;
    }

    public OperationGroup optimizedForInsertCopy() {
        OperationGroup og = new OperationGroup();
        og.entities.addAll(entities);
        List<Entity> copy = og.entities;
        for (int i = 1; i < copy.size(); i++) {
            int iSameType = indexOfSameTypeHigherUp(copy, i);
            if (iSameType >= 0 && iSameType != i - 1 && moveTypeUp(copy, i, iSameType, false)) {
                i--;
            }
        }
        return og;
    }

    public OperationGroup optimizedForUpdateCopy() {
        OperationGroup og = new OperationGroup();
        og.entities.addAll(entities);
        List<Entity> copy = og.entities;
        for (int i = 1; i < copy.size(); i++) {
            int iSameType = indexOfSameTypeHigherUp(copy, i);
            if (iSameType >= 0 && iSameType != i - 1 && moveTypeUp(copy, i, iSameType, true)) {
                i--;
            }
        }
        return og;
    }

    public OperationGroup optimizedForDeleteCopy() {
        OperationGroup og = new OperationGroup();
        og.entities.addAll(entities);
        Collections.reverse(og.entities);
        og = og.optimizedForInsertCopy();
        Collections.reverse(og.entities);
        return og;
    }

    private int indexOfSameTypeHigherUp(List<Entity> copy, int index) {
        final EntityType entityType = copy.get(index).getEntityType();
        for (int i = index - 1; i >= 0; i--) {
            if (entityType == copy.get(i).getEntityType()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Will move the entity at index to beside indexOfSameType as long
     * as dependency analysis
     */
    private boolean moveTypeUp(List<Entity> copy, int index, int indexOfSameType, boolean forceIt) {
        if (!forceIt) {
            for (int i = index - 1; i > indexOfSameType; i--) {
                if (dependsOn(copy, index, i)) {
                    return false;
                }
            }
        }
        LOG.debug("Moving " + copy.get(index) + " at " + index + " up to " + copy.get(indexOfSameType) + " at " + (indexOfSameType + 1));
        copy.add(indexOfSameType + 1, copy.remove(index));
        return true;
    }

    private boolean dependsOn(List<Entity> copy, int index, int possibleDepIndex) {
        Entity entity = copy.get(index);
        Entity possibleDep = copy.get(possibleDepIndex);
        return dependsOn(entity, possibleDep);
    }

    private boolean dependsOn(Entity entity, Entity possibleDep) {
        /*
         * if the possibleDep is reachable through a refnode then
         * it is a dep, otherwise not
         */
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            if (refNode.getReference() == null) {
                /*
                 * No reference set, skip..
                 */
                continue;
            }
            if (refNode.getReference() == possibleDep) {
                LOG.debug(entity + " has FK dependency on " + possibleDep);
                return true;
            }
            else {
                if (dependsOn(refNode.getReference(), possibleDep)) {
                    LOG.debug(entity + " has indirect FK dependency on " + possibleDep + " via " + refNode.getReference());
                    return true;
                }
            }
        }
        LOG.debug(entity + " has no FK dependency on " + possibleDep);
        return false;
    }

}
