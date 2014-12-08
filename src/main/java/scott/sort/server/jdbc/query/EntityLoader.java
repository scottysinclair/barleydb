package scott.sort.server.jdbc.query;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityState;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.NotLoaded;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.exception.execution.TypeConversionException;
import scott.sort.api.exception.execution.TypeConverterNotFoundException;
import scott.sort.api.exception.execution.jdbc.SortJdbcException;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
import scott.sort.api.exception.execution.query.InvalidNodeTypeException;
import scott.sort.api.exception.execution.query.ResultDataConversionException;
import scott.sort.api.exception.execution.query.SortQueryException;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.converter.TypeConverter;

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

    private final EntityLoaders entityLoaders;
    private final List<ProjectionColumn> myProjectionCols;
    private final QueryObject<?> queryObject;
    private final EntityContext entityContext;
    private final ResultSet resultSet;
    private final Map<Integer, Object> rowCache;
    private final LinkedHashMap<Object,Entity> loadedEntities;

    public EntityLoader(EntityLoaders entityLoaders, Projection projection, QueryObject<?> queryObject,
            ResultSet resultSet, EntityContext entityContext) {
        this.entityLoaders = entityLoaders;
        this.resultSet = resultSet;
        this.queryObject = queryObject;
        this.entityContext = entityContext;
        this.loadedEntities = new LinkedHashMap<>();
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
     * @throws InvalidNodeTypeException
     */
    public boolean isEntityThere() throws SortJdbcException, SortQueryException {
        return getEntityKey(false) != null;
    }

    public boolean isNotYetLoaded() throws SortJdbcException, SortQueryException  {
        return !loadedEntities.containsKey( getEntityKey(true) );
    }

    public EntityType getEntityType() {
        return myProjectionCols.get(0).getNodeType().getEntityType();
    }

    public List<Entity> getLoadedEntities() {
        return new ArrayList<>(loadedEntities.values());
    }

    public Object getEntityKey(boolean mustExist) throws SortJdbcException, SortQueryException {
        for (ProjectionColumn column : myProjectionCols) {
            if (column.getNodeType().isPrimaryKey()) {
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
     * @throws InvalidNodeTypeException
     */
    public void associateExistingEntity() throws SortQueryException, SortJdbcException {
        Entity entity = entityContext.getEntity(getEntityType(), getEntityKey(true), false);
        if (entity == null) {
            throw new IllegalQueryStateException("Entity with type "
                    + getEntityType() + " and key " + getEntityKey(true)
                    + " must exist in the entity context");
        }
        if (!loadedEntities.containsKey(entity.getKey().getValue())) {
            LOG.debug("Associating existing entity " + entity);
            loadedEntities.put(entity.getKey().getValue(), entity);
        }
    }

    public Entity load() throws SortQueryException, SortJdbcException {
        Entity entity = entityContext.getOrCreate(getEntityType(), getEntityKey(true));
        /*
         * If the entity state is NOTLOADED, then the entityContext just created it.
         * Therefore we can pre-init each ValueNode to NOTLOADED
         */
        if (entity.getEntityState() == EntityState.NOTLOADED) {
            for (ValueNode node : entity.getChildren(ValueNode.class)) {
                if (entity.getKey() != node) {
                    node.setValue(NotLoaded.VALUE);
                }
            }
        }
        entity.setEntityState(EntityState.LOADING);
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
        loadedEntities.put(entity.getKey().getValue(), entity);
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
        final NodeType nd = column.getNodeType();
        final Integer index = column.getIndex();
        if (nd.getJdbcType() == null) {
            throw new InvalidNodeTypeException(nd, "Node Definition " + nd + " must have a JDBC type");
        }

        JavaType javaType = column.getNodeType().getJavaType();
        if (javaType == null && column.getNodeType().getRelationInterfaceName() != null) {
            /*
             * If there is no java type then it must be a 1:1 relation (RefNode)
             * A 1:N relation does not have a projection column
             */
            EntityType entityType = entityContext.getDefinitions().getEntityTypeMatchingInterface(column.getNodeType().getRelationInterfaceName(), true);
            javaType = entityType.getNodeType(entityType.getKeyNodeName(), true).getJavaType();
            if (javaType == null) {
                throw new InvalidNodeTypeException(nd, "Could not get javaType for projection column " + column);
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
            if (rs.wasNull()) {
                return null;
            }
            else {
                return convertValue(nd, value, javaType);
            }
        }
        catch (SQLException x) {
            throw new SortJdbcException("SQLException getting object from resultset", x);
        }
    }

    private Object convertValue(NodeType nd, Object value, JavaType javaType) throws SortQueryException {
        if  (value == null) {
            return null;
        }
        
        /*
         * If we have a configured type conversion
         * then use it first
         */
        TypeConverter converter;
		try {
			converter = getTypeConverter( nd );
		} 
		catch (TypeConverterNotFoundException e) {
			throw new IllegalQueryStateException("Type converter " + nd.getTypeConverterFqn() + " missing");
		}
        if (converter != null) {
        	/*
        	 * We convert backwards when getting data from the database
        	 */
        	try {
				value = converter.convertBackwards(value);
			} 
        	catch (TypeConversionException e) {
        		throw new IllegalQueryStateException("Type conversion error", e);
			}
        	javaType = converter.getBackwardsJavaType();
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
                    result =  convertToString(nd.getJdbcType(), value);
                    break;
                case UTIL_DATE:
                    result = convertToUtilDate(value);
                    break;
                case UUID:
                    result = convertToUuid(value);
                    break;
                case BYTE_ARRAY:
                    result = convertToByteArray(value);
                    break;
                default:
                   throw new InvalidNodeTypeException(nd, "Java type " + javaType + " is not supported");
               }
        }
        if (result == null) {
            throw new ResultDataConversionException("Could not convert value " + value + " of type " + value.getClass().getName() + " to " + javaType);
        }
        return result;
    }

    private TypeConverter getTypeConverter(NodeType nd) throws TypeConverterNotFoundException  {
    	String typeConverterFqn = nd.getTypeConverterFqn();
    	if (typeConverterFqn == null) {
    		return null;
    	}
    	TypeConverter tc = entityLoaders.getTypeConverter(typeConverterFqn);
    	if (tc == null) {
    		throw new TypeConverterNotFoundException("Could not find type converter '" + typeConverterFqn + "'");
    	}
    	return tc;
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
    private <E extends Enum<E>> Object convertToEnum(NodeType nd, Object value) throws IllegalQueryStateException {
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
        if (value instanceof String) {
            return Long.parseLong((String)value);
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

    private String convertToString(JdbcType jdbcType, Object value) {
        if (value instanceof String) {
            String str = (String)value;
            if (jdbcType == JdbcType.CHAR) {
                str = str.trim();
            }
            return str;
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

    private UUID convertToUuid(Object value) {
        if (value instanceof String) {
            UUID.fromString((String)value);
        }
        return null;
    }

    private byte[] convertToByteArray(Object value) {
        if (byte[].class.isAssignableFrom( value.getClass() )) {
            return (byte[])value;
        }
        return null;
    }

}
