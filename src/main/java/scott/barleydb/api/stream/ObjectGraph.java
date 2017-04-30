package scott.barleydb.api.stream;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * an result item which came out of a query execution
 */
public class ObjectGraph {

    /**
     * The list of entity data which constitutes this query result.
     *  (all of the entity data from all the outer joins 1:1 or 1:N.
     */
    private final List<EntityData> entityData = new LinkedList<>();

    private final Set<NodeId> fetchedToManyNodes = new HashSet<>();

    public void add(EntityData ed) {
        entityData.add( ed );
    }

    public void addAll(Collection<EntityData> ed) {
        entityData.addAll( ed );
    }


    public void setFetched(String entityType, Object entityKey, String nodeName) {
        fetchedToManyNodes.add(new NodeId(entityType, entityKey, nodeName));
    }

    public List<EntityData> getEntityData() {
        return entityData;
    }

    public Collection<NodeId> getFetchedToManyNodes() {
        return fetchedToManyNodes;
    }

    @Override
    public String toString() {
        return "ObjectGraph [entityData=" + entityData + ", fetchedToManyNodes=" + fetchedToManyNodes + "]";
    }

    /**
     * Identifies a specific Node of a specific EntityType
     * @author scott
     *
     */
    public static class NodeId {
        private final String entityType;
        private final Object entityKey;
        private final String nodeName;

        public NodeId(String entityType, Object entityKey, String nodeName) {
            this.entityType = entityType;
            this.entityKey = entityKey;
            this.nodeName = nodeName;
        }

        public String getEntityType() {
            return entityType;
        }

        public Object getEntityKey() {
            return entityKey;
        }

        public String getNodeName() {
            return nodeName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((entityKey == null) ? 0 : entityKey.hashCode());
            result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
            result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NodeId other = (NodeId) obj;
            if (entityKey == null) {
                if (other.entityKey != null)
                    return false;
            } else if (!entityKey.equals(other.entityKey))
                return false;
            if (entityType == null) {
                if (other.entityType != null)
                    return false;
            } else if (!entityType.equals(other.entityType))
                return false;
            if (nodeName == null) {
                if (other.nodeName != null)
                    return false;
            } else if (!nodeName.equals(other.nodeName))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "NodeId [entityType=" + entityType + ", entityKey=" + entityKey + ", nodeName=" + nodeName + "]";
        }
    }

}
