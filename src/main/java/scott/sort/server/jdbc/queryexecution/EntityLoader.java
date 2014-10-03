package scott.sort.server.jdbc.queryexecution;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeDefinition;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityState;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.NotLoaded;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.query.QueryObject;

/**
 *
 * Loads entities for a specific query object
 *
 * @author scott
 *
 */
final class EntityLoader {

    private static final Object NULL_VALUE = new Object();

    private static final Logger LOG = LoggerFactory.getLogger(EntityLoader.class);

    @SuppressWarnings("unused")
    private final EntityLoaders entityLoaders;
    private final List<ProjectionColumn> myProjectionCols;
    private final QueryObject<?> queryObject;
    private final EntityContext entityContext;
    private final ResultSet resultSet;
    private final Map<Integer, Object> rowCache;
    private final List<Entity> loadedEntities;

    public EntityLoader(EntityLoaders entityLoaders, Projection projection, QueryObject<?> queryObject,
            ResultSet resultSet, EntityContext entityContext) {
        this.entityLoaders = entityLoaders;
        this.resultSet = resultSet;
        this.queryObject = queryObject;
        this.entityContext = entityContext;
        this.loadedEntities = new LinkedList<>();
        this.rowCache = new HashMap<>();
        this.myProjectionCols = projection.getColumnsFor(queryObject);
    }

    public QueryObject<?> getQueryObject() {
        return queryObject;
    }

    /**
     * checks if there is an entity that we can load
     *
     * @return
     */
    public boolean isEntityThere() throws SQLException {
        return getEntityKey(false) != null;
    }

    public boolean isNotYetLoaded() throws SQLException {
        Entity entity = entityContext.getEntity(getEntityType(),
                getEntityKey(true), false);
        boolean value = entity == null
                || entity.getEntityState() == EntityState.NOTLOADED;
        return value;
    }

    public EntityType getEntityType() {
        return myProjectionCols.get(0).getNodeDefinition().getEntityType();
    }

    public List<Entity> getLoadedEntities() {
        return loadedEntities;
    }

    public Object getEntityKey(boolean mustExist) throws SQLException {
        for (ProjectionColumn column : myProjectionCols) {
            if (column.getNodeDefinition().isPrimaryKey()) {
                Object value = getValue(column);
                if (mustExist && value == null) {
                    throw new IllegalStateException(
                            "Primary key cannot be null for: "
                                    + getEntityType());
                }
                // if (value instanceof String) {
                // throw new
                // IllegalStateException("testing: Primary key is string .. : "
                // + getEntityType());
                // }
                return value;
            }
        }
        throw new IllegalStateException(
                "Cannot find primary key node definition for: "
                        + getEntityType());
    }

    /**
     * Associates an existing entity in the context with queryobject
     *
     * @throws SQLException
     */
    public void associateExistingEntity() throws SQLException {
        Entity entity = entityContext.getEntity(getEntityType(),
                getEntityKey(true), false);
        if (entity == null) {
            throw new IllegalStateException("Entity with type "
                    + getEntityType() + " and key " + getEntityKey(true)
                    + " must exist in the entity context");
        }
        if (!loadedEntities.contains(entity)) {
            LOG.debug("Associating existing entity " + entity);
            loadedEntities.add(entity);
        }
    }

    public Entity load() throws SQLException {
        Entity entity = entityContext.getOrCreate(getEntityType(), getEntityKey(true));
        entity.setEntityState(EntityState.LOADING);
        for (ValueNode node : entity.getChildren(ValueNode.class)) {
            if (entity.getKey() != node) {
                node.setValue(NotLoaded.VALUE);
            }
        }
        for (ProjectionColumn column : myProjectionCols) {
            Object value = getValue(column);
            Node node = entity.getChild(column.getProperty(), Node.class);
            if (node instanceof ValueNode) {
                ((ValueNode) node).setValue(value);
            } else if (node instanceof RefNode) {
                ((RefNode) node).setEntityKey(value);
            }
            // nothing to set on a ToMany node.
        }
        loadedEntities.add(entity);
        return entity;
    }

    public void clearRowCache() {
        rowCache.clear();
    }

    public Object getValue(ProjectionColumn column) throws SQLException {
        final int index = column.getIndex();
        Object value = rowCache.get(index);
        if (value == null) {
            value = getResultSetValue(resultSet, column);
            value = convertIfRequired(column, value);
            LOG.debug(String.format("%-5s%-15s = %s", index, column.getColumn(),
                    String.valueOf(value)));
            rowCache.put(column.getIndex(), value != null ? value : NULL_VALUE);
        }
        return value != NULL_VALUE ? value : null;
    }

    @SuppressWarnings("incomplete-switch")
    //we fall through and fail at the bottom
    private Object getResultSetValue(ResultSet rs, ProjectionColumn column)
            throws SQLException {
        final NodeDefinition nd = column.getNodeDefinition();
        final Integer index = column.getIndex();
        if (nd.getJdbcType() == null) {
            throw new SQLException("Invalid Node Definition" + nd);
        }

        switch (nd.getJdbcType()) {
        case BIGINT:
        case INT:
        case VARCHAR:
        case NVARCHAR:
            return rs.getObject(index);
        case DECIMAL:
            switch (nd.getJavaType()) {
            case BIGDECIMAL:
                return rs.getBigDecimal(index);
            }

        case TIMESTAMP:
            switch (nd.getJavaType()) {
            case LONG: {
                Timestamp ts = rs.getTimestamp(index);
                return ts != null ? (Long) ts.getTime() : null;
            }
            case UTIL_DATE: {
                Timestamp ts = rs.getTimestamp(index);
                return ts != null ? new Date(ts.getTime()) : null;
            }
            default:
                throw new SQLException("Invalid JDBC type " + nd.getJdbcType());
            }
        case DATE:
            switch (nd.getJavaType()) {
            case LONG: {
                java.sql.Date date = rs.getDate(index);
                return date != null ? (Long) date.getTime() : null;
            }
            case UTIL_DATE: {
                java.sql.Date date = rs.getDate(index);
                return date != null ? new java.util.Date(date.getTime()) : null;
            }
            case SQL_DATE: {
                java.sql.Date date = rs.getDate(index);
                return date;
            }
            default:
                throw new SQLException("Invalid JDBC type " + nd.getJdbcType());
            }
        }
        throw new SQLException("Invalid JDBC type " + nd.getJdbcType());
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> Object convertIfRequired(ProjectionColumn column, Object value) {
        if (value == null) {
            return null;
        }
        JavaType javaType = column.getNodeDefinition().getJavaType();
        if (javaType == null && column.getNodeDefinition().getRelationInterfaceName() != null) {
            /*
             * If there is no java type then it must be a 1:1 relation (RefNode)
             * A 1:N relation does not have a projection column
             */
            EntityType entityType = entityContext.getDefinitions().getEntityTypeMatchingInterface(column.getNodeDefinition().getRelationInterfaceName(), true);
            javaType = entityType.getNode(entityType.getKeyNodeName(), true).getJavaType();
            if (javaType == null) {
                throw new IllegalStateException("Could not get javaType for projection column " + column);
            }
        }
        NodeDefinition nodeDefinition = column.getNodeDefinition();
        if (nodeDefinition.getEnumType() != null) {
            if (value instanceof Number) {
                for (Enum<E> e : java.util.EnumSet.allOf((Class<E>) nodeDefinition
                        .getEnumType())) {
                    if (((Integer) e.ordinal()).equals(((Number)value).intValue())) {
                        LOG.debug("Converted " + value + " to " + e);
                        return e;
                    }
                }
            }
            throw new IllegalStateException("Could not convert from enum");
        }
        if (nodeDefinition.getJavaType() == JavaType.BOOLEAN) {
            switch (nodeDefinition.getJdbcType()) {
            case INT:
                value = value.equals(Integer.valueOf(1));
                break;
            case BIGINT:
                value = value.equals(Long.valueOf(1));
                break;
            default:
                throw new IllegalStateException(
                        "We only convert INT and BIGINT to boolean");
            }
        }
        if (value instanceof BigDecimal) {
            switch(javaType) {
                case LONG: return ((BigDecimal)value).longValue();
                case INTEGER: return ((BigDecimal)value).intValue();
            }
        }
        return value;
    }

}
