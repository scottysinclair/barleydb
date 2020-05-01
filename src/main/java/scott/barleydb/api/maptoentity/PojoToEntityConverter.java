package scott.barleydb.api.maptoentity;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2020 Scott Sinclair
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

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.*;
import scott.barleydb.api.dto.BaseDto;
import scott.barleydb.api.dto.DtoHelper;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PojoToEntityConverter {

    private final EntityContext ctx;
    private final Map<ProperyKey,Method> methodCache = new HashMap<>();

    public PojoToEntityConverter(EntityContext ctx) {
        this.ctx = ctx;
    }

    public Entity toEntity(Object object, String tableName) {
        return toEntity(object, getEntityTypeForTable(tableName));
    }

    public Entity toEntity(Object object, EntityType entityType) {
        if (object == null) {
            return null;
        }
        Map<String,?> data = getProperties(object, entityType);
        NodeType primaryKeyNodeType = entityType.getNodeType(entityType.getKeyNodeName(), true);
        Object pkValue = data.get(primaryKeyNodeType.getName());
        Entity entity;
        if (pkValue != null) {
            entity = ctx.newEntity(entityType, pkValue);
        }
        else {
            entity = ctx.newEntity(entityType);
        }

        for (Node node:  entity.getChildren()) {
            if (node.getNodeType().isPrimaryKey()) {
                continue;
            }
            else if (node instanceof ValueNode) {
                ((ValueNode) node).setValue( convertValue(node, data.get(node.getName())));
            }
            else if (node instanceof RefNode) {
                RefNode rn = (RefNode)node;
                Object fkEntity = data.get(node.getName());
                if (!(fkEntity instanceof String || fkEntity instanceof Number)) {
                    rn.setReference( toEntity(fkEntity, rn.getEntityType()));
                }
            }
            else if (node instanceof ToManyNode) {
                ToManyNode tmn = (ToManyNode) node;
                List<Object> list = (List<Object>)data.get(node.getName());
                if (list != null) {
                    for (Object entityData: list) {
                        Entity e = toEntity(entityData, tmn.getEntityType());
                        if (e != null) {
                            String backRefNode = tmn.getNodeType().getForeignNodeName();
                            e.getChild(backRefNode, RefNode.class).setReference(entity);
                            tmn.getList().add(e);
                        }
                    }
                }
                toManyNodeProcessed(tmn);
            }
        }
        return entity;
    }


    protected void toManyNodeProcessed(ToManyNode toManyNode) {
    }

    protected Object convertValue(Node node, Object value) {
        return value;
    }

    private EntityType getEntityTypeForTable(String tableName) {
        for (EntityType et: ctx.getDefinitions().getEntityTypes()) {
            if (et.getTableName() != null && et.getTableName().equals(tableName)) {
                return et;
            }
        }
        return null;
    }

    public Map<String,Object> getProperties(Object object, EntityType entityType) {
        Map<String,Object> result = new HashMap<String, Object>();
        for (NodeType nodeType: entityType.getNodeTypes()) {
                Object value = callPropertyGetter(object, nodeType);
                result.put(nodeType.getName(), value);
        }
        return result;
    }

    public <T> T callPropertyGetter(Object object, NodeType nodeType) {
        Method method = findMethod(object, nodeType, true);
        return (T)invoke(method, object);
    }

    private Object invoke(Method method, Object dto, Object ...args) {
        try {
            return method.invoke(dto, args);
        }
        catch(InvocationTargetException x) {
            throw new BarleyDBRuntimeException("DTO method '" + method.getName()+ "'  threw exception", x.getTargetException());
        }
        catch(IllegalAccessException x) {
            throw new BarleyDBRuntimeException("Error accessing DTO method '" + method.getName()+ "'", x);
        }
    }

    private Method findMethod(Object dto, NodeType nodeType, boolean getter) {
        ProperyKey key = new ProperyKey(dto.getClass().getName(), nodeType.getName(), getter);
        Method method = methodCache.get(key);
        if (method != null) {
            return method;
        }
        final String propertyName = nodeType.getName().toLowerCase();
        for (Method m: dto.getClass().getMethods()) {
            final String name = m.getName().toLowerCase();
            if (getter) {
                if (name.equals("get" + propertyName) || name.equals( "is" + propertyName )) {
                    methodCache.put(key, m);
                    return m;
                }
            }
            else {
                if (name.equals("set" + propertyName)) {
                    methodCache.put(key, m);
                    return m;
                }
            }
        }
        throw new BarleyDBRuntimeException("Could not find property for nodeType '" + nodeType.getName() + "'");
    }

    private static class ProperyKey {
        private final String className;
        private final String propertyName;
        private final boolean getter;
        public ProperyKey(String className, String propertyName, boolean getter) {
            this.className = className;
            this.propertyName = propertyName;
            this.getter = getter;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + (getter ? 1231 : 1237);
            result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
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
            ProperyKey other = (ProperyKey) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (getter != other.getter)
                return false;
            if (propertyName == null) {
                if (other.propertyName != null)
                    return false;
            } else if (!propertyName.equals(other.propertyName))
                return false;
            return true;
        }
    }


}
