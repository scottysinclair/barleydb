package scott.barleydb.server.jdbc.query;

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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.exception.execution.TypeConversionException;
import scott.barleydb.api.exception.execution.TypeConverterNotFoundException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.exception.execution.query.InvalidNodeTypeException;
import scott.barleydb.api.exception.execution.query.ResultDataConversionException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.EntityData;
import scott.barleydb.server.jdbc.converter.TypeConverter;

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
    private final ResultSet resultSet;
    private final Map<Integer, Object> rowCache;
    private final LinkedHashMap<EntityKey, EntityData> loadedEntityData;;

    public EntityLoader(EntityLoaders entityLoaders, Projection projection, QueryObject<?> queryObject,
            ResultSet resultSet) {
        this.entityLoaders = entityLoaders;
        this.resultSet = resultSet;
        this.queryObject = queryObject;
        this.rowCache = new HashMap<>();
        this.myProjectionCols = projection.getColumnsFor(queryObject);
        this.loadedEntityData = new LinkedHashMap<>();
    }

    public QueryObject<?> getQueryObject() {
        return queryObject;
    }

    public LinkedHashMap<EntityKey, EntityData> getLoadedEntityData() {
        return loadedEntityData;
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
        EntityKey key = new EntityKey(getEntityType(), getEntityKey(true));
        return !entityLoaders.getLoadedEntityData().containsKey( key );
    }

    public EntityType getEntityType() {
        return myProjectionCols.get(0).getNodeType().getEntityType();
    }

    public void clearLoadedEntityData() {
        loadedEntityData.clear();
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

    public EntityData load() throws SortQueryException, SortJdbcException {
        final EntityType entityType = getEntityType();

        EntityData entityData = new EntityData();
        entityData.setNamespace(entityType.getDefinitions().getNamespace());
        entityData.setEntityType(entityType.getInterfaceName());
        entityData.setConstraints( EntityConstraint.mustExistInDatabase() );
        entityData.setEntityState(EntityState.LOADED);

        for (ProjectionColumn column : myProjectionCols) {
            Object value = getValue(column);
            entityData.getData().put(column.getProperty(), value);
        }
        EntityKey key = new EntityKey(entityType, getKey(entityData, entityType));
        entityLoaders.getLoadedEntityData().put(key, entityData);
        loadedEntityData.put(key, entityData);
        return entityData;
    }

    public void associateAsLoaded() throws SortJdbcException, SortQueryException {
        final EntityType entityType = getEntityType();
        EntityKey key = new EntityKey(entityType, getEntityKey(true));
        EntityData entityData = entityLoaders.getLoadedEntityData().get(key);
        if (entityData == null) {
            //we only associate if the entity was already loaded, something went wrong...
            throw new SortQueryException("Could not find entity data with " + entityType + " and key " + key);
        }
        loadedEntityData.put(key, entityData);
    }

    private static Object getKey(EntityData entityData, EntityType entityType) {
        return entityData.getData().get( entityType.getKeyNodeName() );
    }

    public void clearRowCache() {
        rowCache.clear();
    }

    public Object getValue(ProjectionColumn column) throws SortJdbcException, SortQueryException {
        final int index = column.getIndex();
        Object value = rowCache.get(index);
        if (value == null) {
            value = getResultSetValue(resultSet, column);
            LOG.debug(String.format("%-5s%-20s%-15s = %s", index, column.getNodeType().getEntityType().getInterfaceShortName(), column.getColumn(),
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
            EntityType entityType = entityLoaders.getDefinitions().getEntityTypeMatchingInterface(column.getNodeType().getRelationInterfaceName(), true);
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
                throw new IllegalQueryStateException("Type conversion error for column " + nd.getColumnName(), e);
            }
            javaType = converter.getBackwardsJavaType();
        }

        Object result = null;
        if (nd.getEnumSpec() != null) {
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
                    /*
                     * it looks like we should add fallback
                     * for when the enum classes do not exist, so that
                     * everything can work in a fully dynamic way..
                     */
                    //result = convertToInteger(value);
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
                case SHORT: {
                     result = convertToShort(value);
                     break;
                }
                default:
                   throw new InvalidNodeTypeException(nd, "Java type " + javaType + " is not supported");
               }
        }
        if (result == null) {
            if (nd.getEnumSpec() == null) {
                throw new ResultDataConversionException("Could not convert value " + value + " of type " + value.getClass().getName() + " to " + javaType);
            }
            else {
                throw new ResultDataConversionException("Could not convert value " + value + " of type " + value.getClass().getName() + " to " + nd.getEnumSpec().getClassName());
            }
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

    /**
     * Converts to an enum value if the enum class exists
     * otherwise converts to a string representation of the enum value.
     * This way enums are supported with and without generated classes.
     *
     * @param nd
     * @param value
     * @return
     * @throws IllegalQueryStateException
     */
    private <E extends Enum<E>> Object convertToEnum(NodeType nd, Object value) throws IllegalQueryStateException {
        Object result = NodeType.convertToEnum(nd, value);
        if (result != null) {
            return result;
        }
        throw new IllegalQueryStateException("Could not convert value '" + value + " to enum of type " + nd.getEnumSpec().getClassName());
    }

    private Integer convertToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return null;
    }

    private Short convertToShort(Object value) {
      if (value instanceof Number) {
        return ((Number)value).shortValue();
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
        if (value instanceof java.sql.Date) {
            //a java.sql.Date IS A java.util.Date, but we create a fresh java.util.Date to avoid
            //any possible side-effects.
            return new Date(((java.sql.Date)value).getTime());
        }
        if (value instanceof Timestamp) {
            //a Timestamp IS A java.util.Date, but we create a fresh java.util.Date to avoid
            //any possible side-effects.
            return new Date(((Timestamp)value).getTime());
        }
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
