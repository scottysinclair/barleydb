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
import scott.sort.api.config.NodeDefinition;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.exception.PreparingPersistStatementException;

public class PreparedStatementHelper {

    private final Definitions definitions;

    public PreparedStatementHelper(Definitions definitions) {
        this.definitions = definitions;
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node) throws PreparingPersistStatementException {
        final NodeDefinition nd = node.getNodeDefinition();
        if (node instanceof RefNode) {
            setParameter(ps, index, nd, ((RefNode) node).getEntityKey());
        }
        else if (node instanceof ValueNode) {
            setParameter(ps, index, nd, ((ValueNode) node).getValue());
        }
        else {
            throw new IllegalStateException("Cannot set parameter for node '" + node + "'");
        }
    }

    public void setParameter(final PreparedStatement ps, final int index, final Node node, final Object value) throws PreparingPersistStatementException {
        setParameter(ps, index, node.getNodeDefinition(), value);
    }

    public void setParameter(final PreparedStatement ps, final int index, final NodeDefinition nd, final Object value) throws PreparingPersistStatementException {
        if (value == null) {
            try {
                setNull(ps, index, nd.getJdbcType());
            } catch (SQLException x) {
                throw new PreparingPersistStatementException("Could not set NULL on column " + nd.getColumnName() + " for table " + nd.getEntityType().getTableName(), x);
            }
            return;
        }

        JavaType javaType = getJavaType(nd);
        JdbcType jdbcType = getJdbcType(nd);

        try {
            setValue(ps, index, javaType, jdbcType, value);
        } catch (SQLException x) {
            throw new PreparingPersistStatementException("Could not set value " + value + " on column " + nd.getColumnName() + " for table " + nd.getEntityType().getTableName(), x);
        }
    }

    private JavaType getJavaType(NodeDefinition nd) {
        if (nd.getJavaType() != null) {
            return nd.getJavaType();
        }
        if (nd.getRelationInterfaceName() != null) {
            final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            final NodeDefinition nd2 = et.getNode(et.getKeyNodeName(), true);
            return nd2.getJavaType();
        }
        if (nd.getEnumType() != null) {
            return JavaType.ENUM;
        }
        throw new IllegalStateException(nd + " has no javatype");
    }

    private JdbcType getJdbcType(NodeDefinition nd) {
        if (nd.getJdbcType() != null) {
            return nd.getJdbcType();
        }
        if (nd.getRelationInterfaceName() != null) {
            final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            final NodeDefinition nd2 = et.getNode(et.getKeyNodeName(), true);
            return nd2.getJdbcType();
        }
        throw new IllegalStateException(nd + " has no jdbctype");
    }

    private final void setValue(PreparedStatement ps, int index, JavaType javaType, JdbcType jdbcType, Object value) throws SQLException, PreparingPersistStatementException  {
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
            throw new PreparingPersistStatementException("Java type " + javaType + " is not supported");
        }
    }

    private void setEnum(PreparedStatement ps, int index, JdbcType jdbcType, Enum<? extends Enum<?>> value) throws SQLException {
        switch (jdbcType) {
        case INT:
            ps.setInt(index, value.ordinal());
            break;
        case NVARCHAR:
            ps.setNString(index, value.toString());
        case VARCHAR:
            ps.setString(index, value.toString());
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setBigDecimal(PreparedStatement ps, int index, JdbcType jdbcType, BigDecimal value) throws SQLException {
        ps.setBigDecimal(index, value);
    }

    private void setBoolean(PreparedStatement ps, int index, JdbcType jdbcType, Boolean value) throws SQLException {
        ps.setBoolean(index, value);
    }

    private void setInteger(PreparedStatement ps, int index, JdbcType jdbcType, Integer value) throws SQLException {
        switch (jdbcType) {
        case INT:
            ps.setInt(index, value);
            break;
        case TIMESTAMP:
            ps.setTimestamp(index, new java.sql.Timestamp((long) (int) value));
            break;
        case DATE:
            ps.setDate(index, new java.sql.Date((long) (int) value));
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setLong(PreparedStatement ps, int index, JdbcType jdbcType, Long value) throws SQLException {
        switch (jdbcType) {
        case BIGINT:
            ps.setLong(index, value);
            break;
        case TIMESTAMP:
            ps.setTimestamp(index, new java.sql.Timestamp(value));
            break;
        case DATE:
            ps.setDate(index, new java.sql.Date(value));
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setSqlDate(PreparedStatement ps, int index, JdbcType jdbcType, java.sql.Date value) throws SQLException {
        switch (jdbcType) {
        case DATE:
            ps.setDate(index, value);
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setString(PreparedStatement ps, int index, JdbcType jdbcType, String value) throws SQLException {
        switch (jdbcType) {
        case NVARCHAR:
            ps.setNString(index, value);
            break;
        case VARCHAR:
            ps.setString(index, value);
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void setUtilDate(PreparedStatement ps, int index, JdbcType jdbcType, Date value) throws SQLException {
        switch (jdbcType) {
        case TIMESTAMP:
            ps.setTimestamp(index, new java.sql.Timestamp((Long) value.getTime()));
            break;
        case DATE:
            ps.setDate(index, new java.sql.Date(value.getTime()));
            break;
        default:
            fail(value, jdbcType);
        }
    }

    private void fail(Object value, JdbcType jdbcType) {
        throw new IllegalStateException("Cannot convert " + value + " to jdbc type " + jdbcType);
    }

    private void setNull(final PreparedStatement ps, final int index, final JdbcType type) throws SQLException {
        ps.setNull(index, toSqlTypes(type));
    }

    private int toSqlTypes(JdbcType type) {
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
        default:
            throw new IllegalStateException("Unsupported jdbctype " + type);
        }
    }

}
