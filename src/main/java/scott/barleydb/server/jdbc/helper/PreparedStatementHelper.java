package scott.barleydb.server.jdbc.helper;

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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.TypeConversionException;
import scott.barleydb.api.exception.execution.TypeConverterNotFoundException;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.converter.TypeConverter;

public abstract class PreparedStatementHelper<PREPARING_EX extends SortException> {

    private static final Logger LOG = LoggerFactory.getLogger(PreparedStatementHelper.class);

    private final JdbcEntityContextServices entityContextServices;
    private final Definitions definitions;

    public PreparedStatementHelper(JdbcEntityContextServices entityContextServices, Definitions definitions) {
        this.entityContextServices = entityContextServices;
        this.definitions = definitions;
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node) throws PREPARING_EX {
        final NodeType nd = node.getNodeType();
        if (node instanceof RefNode) {
            setParameter(ps, index, nd, ((RefNode) node).getEntityKey());
        }
        else if (node instanceof ValueNode) {
            setParameter(ps, index, nd, ((ValueNode) node).getValue());
        }
        else {
            throw newPreparingStatementException("Cannot set parameter for node '" + node + "'");
        }
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node, final Object value) throws PREPARING_EX {
        setParameter(ps, index, node.getNodeType(), value);
    }

    public void setParameter(final PreparedStatement ps, final int index, final NodeType nd, Object value) throws PREPARING_EX {
        if (value == null) {
            setNull(ps, index, nd.getJdbcType());
            return;
        }
        JavaType javaType = getJavaType(nd);
        JdbcType jdbcType = getJdbcType(nd);
        TypeConverter converter;
        try {
            converter = getTypeConverter( nd );
        }
        catch (TypeConverterNotFoundException e) {
            throw newPreparingStatementException("Could not find type converter '" + nd.getTypeConverterFqn() + "'");
        }
        if (converter != null) {
            /*
             * Perform the configured conversion before setting the JDBC parameter
             */
            try {
                Object oldValue = value;
                value = converter.convertForwards(value);
                LOG.debug("Performing type conversion {} from {} to {}", new Object[]{nd.getTypeConverterFqn(), oldValue, value});
            } catch (TypeConversionException e) {
                throw newPreparingStatementException("Error during type conversion", e);
            }
            javaType = converter.getForwardsJavaType();
        }
        setValue(ps, index, nd.getEnumSpec(), javaType, jdbcType, value);
    }

    private TypeConverter getTypeConverter(NodeType nd) throws TypeConverterNotFoundException {
        String typeConverterFqn = nd.getTypeConverterFqn();
        if (typeConverterFqn == null) {
            return null;
        }
        TypeConverter tc = entityContextServices.getTypeConverter(typeConverterFqn);
        if (tc == null) {
            throw new TypeConverterNotFoundException("Could not find type converter '" + typeConverterFqn + "'");
        }
        return tc;
    }

    private JavaType getJavaType(NodeType nd) throws PREPARING_EX {
        if (nd.getJavaType() != null) {
            return nd.getJavaType();
        }
        if (nd.getRelationInterfaceName() != null) {
            final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            final NodeType nd2 = et.getNodeType(et.getKeyNodeName(), true);
            return nd2.getJavaType();
        }
        if (nd.getEnumType() != null) {
            return JavaType.ENUM;
        }
        throw newPreparingStatementException(nd + " has no javatype");
    }

    private JdbcType getJdbcType(NodeType nd) throws PREPARING_EX {
        if (nd.getJdbcType() != null) {
            return nd.getJdbcType();
        }
        if (nd.getRelationInterfaceName() != null) {
            final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            final NodeType nd2 = et.getNodeType(et.getKeyNodeName(), true);
            return nd2.getJdbcType();
        }
        throw newPreparingStatementException(nd + " has no jdbctype");
    }

    public abstract PREPARING_EX newPreparingStatementException(String message);

    public abstract PREPARING_EX newPreparingStatementException(String message, Throwable cause);

    protected PREPARING_EX newSetValueError(String type, Throwable cause) {
        return newPreparingStatementException("Error seting value of type " + type + " on prepared statement", cause);
    }

    private final void setValue(PreparedStatement ps, int index, EnumSpec enumSpec, JavaType javaType, JdbcType jdbcType, Object value) throws PREPARING_EX  {
        switch (javaType) {
        case ENUM:
            if (value instanceof Enum) {
                @SuppressWarnings("unchecked")
                Enum<? extends Enum<?>> eValue = (Enum<? extends Enum<?>>)value;
                setEnumValue(ps, index, enumSpec, jdbcType, eValue.name());
                return;
            }
            else if (value instanceof String) {
                setEnumValue(ps, index, enumSpec, jdbcType, (String)value);
                return;
            }
            throw newPreparingStatementException("Cannot convert value '" + value + "' to an Enum id");
        case BIGDECIMAL:
            setBigDecimal(ps, index, jdbcType, (BigDecimal) value);
            return;
        case BOOLEAN:
            setBoolean(ps, index, jdbcType, (Boolean) value);
            return;
        case INTEGER:
            setInteger(ps, index, jdbcType, (Integer) value);
            return;
        case LONG:
            setLong(ps, index, jdbcType, (Long) value);
            return;
        case SQL_DATE:
            setSqlDate(ps, index, jdbcType, (java.sql.Date) value);
            return;
        case STRING:
            setString(ps, index, jdbcType, (String) value);
            return;
        case UTIL_DATE:
            setUtilDate(ps, index, jdbcType, (Date) value);
            return;
        case BYTE_ARRAY:
            setByteArray(ps, index, jdbcType, (byte[]) value);
            return;
        default:
            throw newPreparingStatementException("Java type " + javaType + " is not supported");
        }
    }

    private void setEnumValue(PreparedStatement ps, int index, EnumSpec enumSpec, JdbcType jdbcType, String value) throws PREPARING_EX {
        Object enumId = getEnumId(enumSpec, value);
        if (enumId instanceof Integer) {
          setInteger(ps, index, jdbcType, (Integer)enumId);
        }
        else if (enumId instanceof String) {
          setString(ps, index, jdbcType, (String)value);
        }
        else {
            throw newPreparingStatementException("Do not support enum Ids of type '" + value.getClass().getSimpleName() + "' in enumSpec '" + enumSpec.getClassName() + "'");    }
        }

    private Object getEnumId(EnumSpec enumSpec, String value) throws PREPARING_EX {
        for (EnumValueSpec valueEl: enumSpec.getEnumValues()) {
            if (valueEl.getName().equals( value )) {
                return valueEl.getId();
            }
        }
        throw newPreparingStatementException("Could not find enum value '" + value + "' in enumSpec '" + enumSpec.getClassName() + "'");
    }

    private void sxxssx(PreparedStatement ps, int index, JdbcType jdbcType, Enum<? extends Enum<?>> value) throws PREPARING_EX {
        switch (jdbcType) {
        case INT:
            try {
                ps.setInt(index, value.ordinal());
            }
            catch (SQLException x) {
                throw newSetValueError("Int", x);
            }
            break;
        case NVARCHAR:
            try {
                ps.setNString(index, value.toString());
            }
            catch (SQLException x) {
                throw newSetValueError("NString", x);
            }
        case VARCHAR:
            try {
                ps.setString(index, value.toString());
            }
            catch (SQLException x) {
                throw newSetValueError("String", x);
            }
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setBigDecimal(PreparedStatement ps, int index, JdbcType jdbcType, BigDecimal value) throws PREPARING_EX {
        try {
            ps.setBigDecimal(index, value);
        }
        catch (SQLException x) {
            throw newSetValueError("BigDecimal", x);
        }
    }

    private void setBoolean(PreparedStatement ps, int index, JdbcType jdbcType, Boolean value) throws PREPARING_EX {
        try {
            ps.setBoolean(index, value);
        }
        catch (SQLException x) {
            throw newSetValueError("Boolean", x);
        }
    }

    private void setInteger(PreparedStatement ps, int index, JdbcType jdbcType, Integer value) throws PREPARING_EX {
        switch (jdbcType) {
        case INT:
            try {
                ps.setInt(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("Int", x);
            }
            break;
        case TIMESTAMP:
            try {
                ps.setTimestamp(index, new java.sql.Timestamp((long) (int) value));
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Timestamp", x);
            }
            break;
        case DATE:
            try {
                ps.setDate(index, new java.sql.Date((long) (int) value));
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Date", x);
            }
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setLong(PreparedStatement ps, int index, JdbcType jdbcType, Long value) throws PREPARING_EX  {
        switch (jdbcType) {
        case BIGINT:
            try {
                ps.setLong(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("Long", x);
            }
            break;
        case TIMESTAMP:
            try {
                ps.setTimestamp(index, new java.sql.Timestamp(value));
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Timestamp", x);
            }
            break;
        case DATE:
            try {
                ps.setDate(index, new java.sql.Date(value));
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Date", x);
            }
            break;
        case VARCHAR:
        case CHAR:
            try {
                ps.setString(index, value.toString());
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Date", x);
            }
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setSqlDate(PreparedStatement ps, int index, JdbcType jdbcType, java.sql.Date value) throws PREPARING_EX {
        switch (jdbcType) {
        case DATE:
            try {
                ps.setDate(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("java.sql.Date", x);
            }
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setString(PreparedStatement ps, int index, JdbcType jdbcType, String value) throws PREPARING_EX  {
        switch (jdbcType) {
        case NVARCHAR:
            try {
                ps.setNString(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("NString", x);
            }
            break;
        case VARCHAR:
            try {
                ps.setString(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("String", x);
            }
            break;
        case CHAR:
            try {
                ps.setString(index, value);
            }
            catch (SQLException x) {
                throw newSetValueError("String", x);
            }
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setUtilDate(PreparedStatement ps, int index, JdbcType jdbcType, Date value) throws PREPARING_EX {
        switch (jdbcType) {
            case TIMESTAMP:
                try {
                    ps.setTimestamp(index, new java.sql.Timestamp((Long) value.getTime()));
                }
                catch (SQLException x) {
                    throw newSetValueError("java.sql.Timestamp", x);
                }
                break;
            case DATETIME:
                try {
                    //datetime is not in a JDBC type, we map it to java.sql.Timestamp
                    ps.setTimestamp(index, new java.sql.Timestamp((Long) value.getTime()));
                }
                catch (SQLException x) {
                    throw newSetValueError("java.sql.Timestamp", x);
                }
                break;
            case DATE:
                try {
                    ps.setDate(index, new java.sql.Date(value.getTime()));
                }
                catch (SQLException x) {
                    throw newSetValueError("java.sql.Date", x);
                }
                break;
            default:
                fail(value, jdbcType);
        }
    }

    private void setByteArray(PreparedStatement ps, int index, JdbcType jdbcType, byte[] value) throws PREPARING_EX {
        switch (jdbcType) {
            case BLOB:
                try {
                    ps.setBytes(index, value);
                }
                catch (SQLException x) {
                    throw newSetValueError("java.sql.Timestamp", x);
                }
                break;
            default:
                fail(value, jdbcType);
        }
    }


    private void fail(Object value, JdbcType jdbcType) throws PREPARING_EX {
        throw newPreparingStatementException("Cannot convert " + value + " to jdbc type " + jdbcType);
    }

    private void setNull(final PreparedStatement ps, final int index, final JdbcType type) throws PREPARING_EX {
        try {
            ps.setNull(index, toSqlTypes(type));
        }
        catch (SQLException x) {
            throw newPreparingStatementException("SQLException setting null value for JDBC type " + type);
        }
    }

    private int toSqlTypes(JdbcType type) throws PREPARING_EX {
        switch (type) {
        case BIGINT:
            return java.sql.Types.BIGINT;
        case INT:
            return java.sql.Types.INTEGER;
        case VARCHAR:
            return java.sql.Types.VARCHAR;
        case NVARCHAR:
            return java.sql.Types.NVARCHAR;
        case BLOB:
            return java.sql.Types.BLOB;
        case CLOB:
            return java.sql.Types.CLOB;
        case TIMESTAMP:
            return java.sql.Types.TIMESTAMP;
        case DATE:
            return java.sql.Types.DATE;
        case DECIMAL:
            return java.sql.Types.DECIMAL;
        case CHAR:
            return java.sql.Types.CHAR;
        case DATETIME:
            return java.sql.Types.TIMESTAMP;
        default:
            throw newPreparingStatementException("Unsupported jdbctype " + type);
        }
    }

}
