package scott.sort.server.jdbc.helper;

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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.exception.SortException;

public abstract class PreparedStatementHelper<PREPARING_PERSIST_EX extends SortException> {

    private final Definitions definitions;

    public PreparedStatementHelper(Definitions definitions) {
        this.definitions = definitions;
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node) throws PREPARING_PERSIST_EX {
        final NodeType nd = node.getNodeType();
        if (node instanceof RefNode) {
            setParameter(ps, index, nd, ((RefNode) node).getEntityKey());
        }
        else if (node instanceof ValueNode) {
            setParameter(ps, index, nd, ((ValueNode) node).getValue());
        }
        else {
            throw newPreparingPersistStatementException("Cannot set parameter for node '" + node + "'");
        }
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node, final Object value) throws PREPARING_PERSIST_EX {
        setParameter(ps, index, node.getNodeType(), value);
    }

    public void setParameter(final PreparedStatement ps, final int index, final NodeType nd, final Object value) throws PREPARING_PERSIST_EX {
        if (value == null) {
            setNull(ps, index, nd.getJdbcType());
            return;
        }
        JavaType javaType = getJavaType(nd);
        JdbcType jdbcType = getJdbcType(nd);
        setValue(ps, index, javaType, jdbcType, value);
    }

    private JavaType getJavaType(NodeType nd) throws PREPARING_PERSIST_EX {
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
        throw newPreparingPersistStatementException(nd + " has no javatype");
    }

    private JdbcType getJdbcType(NodeType nd) throws PREPARING_PERSIST_EX {
        if (nd.getJdbcType() != null) {
            return nd.getJdbcType();
        }
        if (nd.getRelationInterfaceName() != null) {
            final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            final NodeType nd2 = et.getNodeType(et.getKeyNodeName(), true);
            return nd2.getJdbcType();
        }
        throw newPreparingPersistStatementException(nd + " has no jdbctype");
    }

    public abstract PREPARING_PERSIST_EX newPreparingPersistStatementException(String message);

    public abstract PREPARING_PERSIST_EX newPreparingPersistStatementException(String message, Throwable cause);

    protected PREPARING_PERSIST_EX newSetValueError(String type, Throwable cause) {
        return newPreparingPersistStatementException("Error seting value of type " + type + " on prepared statement", cause);
    }

    private final void setValue(PreparedStatement ps, int index, JavaType javaType, JdbcType jdbcType, Object value) throws PREPARING_PERSIST_EX  {
        switch (javaType) {
        case ENUM:
            @SuppressWarnings("unchecked")
            Enum<? extends Enum<?>> eValue = (Enum<? extends Enum<?>>)value;
            setEnum(ps, index, jdbcType, eValue);
            return;
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
        default:
            throw newPreparingPersistStatementException("Java type " + javaType + " is not supported");
        }
    }

    private void setEnum(PreparedStatement ps, int index, JdbcType jdbcType, Enum<? extends Enum<?>> value) throws PREPARING_PERSIST_EX {
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

    private void setBigDecimal(PreparedStatement ps, int index, JdbcType jdbcType, BigDecimal value) throws PREPARING_PERSIST_EX {
        try {
            ps.setBigDecimal(index, value);
        }
        catch (SQLException x) {
            throw newSetValueError("BigDecimal", x);
        }
    }

    private void setBoolean(PreparedStatement ps, int index, JdbcType jdbcType, Boolean value) throws PREPARING_PERSIST_EX {
        try {
            ps.setBoolean(index, value);
        }
        catch (SQLException x) {
            throw newSetValueError("Boolean", x);
        }
    }

    private void setInteger(PreparedStatement ps, int index, JdbcType jdbcType, Integer value) throws PREPARING_PERSIST_EX {
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

    private void setLong(PreparedStatement ps, int index, JdbcType jdbcType, Long value) throws PREPARING_PERSIST_EX  {
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
        default:
            fail(value, jdbcType);
        }
    }

    private void setSqlDate(PreparedStatement ps, int index, JdbcType jdbcType, java.sql.Date value) throws PREPARING_PERSIST_EX {
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

    private void setString(PreparedStatement ps, int index, JdbcType jdbcType, String value) throws PREPARING_PERSIST_EX  {
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
        default:
            fail(value, jdbcType);
        }
    }

    private void setUtilDate(PreparedStatement ps, int index, JdbcType jdbcType, Date value) throws PREPARING_PERSIST_EX {
        switch (jdbcType) {
        case TIMESTAMP:
            try {
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

    private void fail(Object value, JdbcType jdbcType) throws PREPARING_PERSIST_EX {
        throw newPreparingPersistStatementException("Cannot convert " + value + " to jdbc type " + jdbcType);
    }

    private void setNull(final PreparedStatement ps, final int index, final JdbcType type) throws PREPARING_PERSIST_EX {
        try {
            ps.setNull(index, toSqlTypes(type));
        }
        catch (SQLException x) {
            throw newPreparingPersistStatementException("SQLException setting null value for JDBC type " + type);
        }
    }

    private int toSqlTypes(JdbcType type) throws PREPARING_PERSIST_EX {
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
        default:
            throw newPreparingPersistStatementException("Unsupported jdbctype " + type);
        }
    }

}
