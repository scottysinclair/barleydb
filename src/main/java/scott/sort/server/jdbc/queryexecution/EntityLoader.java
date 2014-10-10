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
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.exception.InvalidNodeDefinitionException;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.query.IllegalQueryStateException;
import scott.sort.api.exception.query.ResultDataConversionException;
import scott.sort.api.exception.query.SortQueryException;
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
     * @throws InvalidNodeDefinitionException
     */
    public boolean isEntityThere() throws SortJdbcException, SortQueryException {
        return getEntityKey(false) != null;
    }

    public boolean isNotYetLoaded() throws SortJdbcException, SortQueryException  {
        Entity entity = entityContext.getEntity(getEntityType(), getEntityKey(true), false);
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

    public Object getEntityKey(boolean mustExist) throws SortJdbcException, SortQueryException {
        for (ProjectionColumn column : myProjectionCols) {
            if (column.getNodeDefinition().isPrimaryKey()) {
                Object value = getValue(column);
                if (mustExist && value == null) {
                    throw new IllegalQueryStateException(
                            "Primary key cannot be null for: "
                                    + getEntityType());
                }
                return value;
            }
        }
        throw new IllegalQueryStateException("Cannot find primary key node definition for: " + getEntityType());
    }

    /**
     * Associates an existing entity in the context with queryobject
     *
     * @throws SQLException
     * @throws SortJdbcException
     * @throws InvalidNodeDefinitionException
     */
    public void associateExistingEntity() throws SortQueryException, SortJdbcException {
        Entity entity = entityContext.getEntity(getEntityType(), getEntityKey(true), false);
        if (entity == null) {
            throw new IllegalQueryStateException("Entity with type "
                    + getEntityType() + " and key " + getEntityKey(true)
                    + " must exist in the entity context");
        }
        if (!loadedEntities.contains(entity)) {
            LOG.debug("Associating existing entity " + entity);
            loadedEntities.add(entity);
        }
    }

    public Entity load() throws SortQueryException, SortJdbcException {
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

    public Object getValue(ProjectionColumn column) throws SortJdbcException, SortQueryException {
        final int index = column.getIndex();
        Object value = rowCache.get(index);
        if (value == null) {
            value = getResultSetValue(resultSet, column);
            LOG.debug(String.format("%-5s%-15s = %s", index, column.getColumn(),
                    String.valueOf(value)));
            rowCache.put(column.getIndex(), value != null ? value : NULL_VALUE);
        }
        return value != NULL_VALUE ? value : null;
    }

    //we fall through and fail at the bottom
    private Object getResultSetValue(ResultSet rs, ProjectionColumn column) throws SortJdbcException, SortQueryException {
        final NodeDefinition nd = column.getNodeDefinition();
        final Integer index = column.getIndex();
        if (nd.getJdbcType() == null) {
            throw new InvalidNodeDefinitionException(nd, "Node Definition " + nd + " must have a JDBC type");
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
                throw new InvalidNodeDefinitionException(nd, "Could not get javaType for projection column " + column);
            }
        }
        try {
            Object value = null;
            if (nd.getJdbcType() == JdbcType.TIMESTAMP) {
                    //FIX for oracle which returns it's own oracle.sql.TIMESTAMP class
                //which does extend java.sql.Timestamp  when you call resultSet.getObject()
                value = rs.getTimestamp(index);
            }
            else {
                value = rs.getObject(index);
            }
            return convertValue(nd, value, javaType);
        }
        catch (SQLException x) {
            throw new SortJdbcException("SQLException getting object from resultset", x);
        }
    }

    private Object convertValue(NodeDefinition nd, Object value, JavaType javaType) throws InvalidNodeDefinitionException, ResultDataConversionException, IllegalQueryStateException {
        if  (value == null) {
            return null;
        }
        Object result = null;
        if (nd.getEnumType() != null) {
            result = convertToEnum(nd, value);
        }
        else {
            switch (javaType) {
                case BIGDECIMAL:
                    result = convertToBigDecimal(value);
                    break;
                case BOOLEAN:
                    result = convertToBoolean(value);
                    break;
                case ENUM:
                    break;
                case INTEGER:
                    result = convertToInteger(value);
                    break;
                case LONG:
                    result = convertToLong(value);
                    break;
                case SQL_DATE:
                    result = convertToSqlDate(value);
                    break;
                case STRING:
                    result =  convertToString(value);
                    break;
                case UTIL_DATE:
                    result = convertToUtilDate(value);
                    break;
                default:
                   throw new InvalidNodeDefinitionException(nd, "Java type " + javaType + " is not supported");
               }
        }
        if (result == null) {
            throw new ResultDataConversionException("Could not convert value " + value + " of type " + value.getClass().getName() + " to " + javaType);
        }
        return result;
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal)value;
        }
        return null;
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean)value;
        }
        if (value instanceof Number) {
            return ((Number)value).intValue() == 1;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> Object convertToEnum(NodeDefinition nd, Object value) throws IllegalQueryStateException {
        if (value instanceof Number) {
            for (Enum<E> e : java.util.EnumSet.allOf((Class<E>) nd.getEnumType())) {
                if (((Integer) e.ordinal()).equals(((Number)value).intValue())) {
                    LOG.debug("Converted " + value + " to " + e);
                    return e;
                }
            }
        }
        throw new IllegalQueryStateException("Could not convert from enum");
    }

    private Integer convertToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return null;
    }

    private Long convertToLong(Object value) {
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp)value).getTime();
        }
        return null;
    }

    private java.sql.Date convertToSqlDate(Object value) {
        if (value instanceof java.sql.Date) {
            return (java.sql.Date)value;
        }
        if (value instanceof Long) {
            return new java.sql.Date((Long)value);
        }
        return null;
    }

    private String convertToString(Object value) {
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }

    private Date convertToUtilDate(Object value) {
        if (value instanceof Date) {
            return (Date)value;
        }
        if (value instanceof Long) {
            return new Date((Long)value);
        }
        return null;
    }

}
