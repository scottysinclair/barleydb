package com.smartstream.morf.server.jdbc.helper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.config.NodeDefinition;
import com.smartstream.morf.api.core.entity.Node;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ValueNode;
import com.smartstream.morf.api.core.types.JavaType;
import com.smartstream.morf.api.core.types.JdbcType;

public class PreparedStatementHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PreparedStatementHelper.class);

	private final Definitions definitions;

	public PreparedStatementHelper(Definitions definitions) {
		this.definitions = definitions;
	}

	/**
	 * Sets the given parameter with the value from the node
	 * @param ps
	 * @param index
	 * @param entity
	 * @param node
	 * @throws SQLException
	 */
	public void setParameter(final PreparedStatement ps, final int index, final Node node) throws SQLException {
		final NodeDefinition nd = node.getNodeDefinition();
		if (node instanceof RefNode) {
		    setParameter(ps, index, nd, ((RefNode)node).getEntityKey());
		}
		else if (node instanceof ValueNode) {
		    setParameter(ps, index, nd, ((ValueNode)node).getValue());
		}
		else {
		    throw new IllegalStateException("Cannot set parameter for node '" + node + "'");
		}
	}

	/**
	 *
	 * Sets the given parameter with the specified value
	 */
	public void setParameter(final PreparedStatement ps, final int index, final Node node, final Object value) throws SQLException {
		setParameter(ps, index, node.getNodeDefinition(), value);
	}

	public void setParameter(final PreparedStatement ps, final int index, final NodeDefinition nd, final Object value) throws SQLException {
		//LOG.debug("setParameter " + index + " " + nd.getName() + " " + value);
		JavaType javaType = nd.getJavaType();
		if (nd.getJavaType() == null) {
			if (nd.getRelationInterfaceName() != null) {
				final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
				final NodeDefinition nd2 = et.getNode(et.getKeyNodeName(), true);
				javaType = nd2.getJavaType();
			}
			else if (nd.getEnumType() != null) {
			      javaType = JavaType.INTEGER;
			}
			else {
				throw new IllegalStateException(nd + " has no javatype");
			}
		}
		JdbcType jdbcType = nd.getJdbcType();
		if (nd.getJdbcType() == null) {
			if (nd.getRelationInterfaceName() != null) {
				final EntityType et = definitions.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
				final NodeDefinition nd2 = et.getNode(et.getKeyNodeName(), true);
				jdbcType = nd2.getJdbcType();
			}
			else {
				throw new IllegalStateException(nd + " has no jdbctype");
			}
		}
		if (value == null) {
			setNull(ps, index, jdbcType);
			return;
		}
		switch(javaType) {
			case LONG:
			      switch(jdbcType) {
                      case BIGINT: ps.setLong(index, (Long)value);
                          break;
                      case TIMESTAMP:
                          ps.setTimestamp(index, new java.sql.Timestamp((Long)value));
                          break;
                      case DATE:
                          ps.setDate(index, new java.sql.Date((Long)value));
                          break;
                       default: throw new IllegalStateException("Cannot convert long to " + jdbcType);
			      }
			      break;
			case INTEGER:
				if (value instanceof Enum) {
					ps.setInt(index, ((Enum<?>)value).ordinal());
					LOG.debug("Converted " + value + " to " + ((Enum<?>)value).ordinal());
				}
				else {
					ps.setInt(index, (Integer)value);
				}
				break;
			case BIGDECIMAL: {
				ps.setBigDecimal(index, (BigDecimal)value);
				break;
			}
			case UTIL_DATE: {
				Date date = (Date)value;
		      switch(jdbcType) {
                      case TIMESTAMP:
                          ps.setTimestamp(index, new java.sql.Timestamp((Long)date.getTime()));
                          break;
                      case DATE:
                          ps.setDate(index, new java.sql.Date(date.getTime()));
                          break;
                       default: throw new IllegalStateException("Cannot convert util date to " + jdbcType);
			      }
				break;
			}
			case STRING:
				switch(jdbcType) {
					case NVARCHAR:
						ps.setNString(index, (String)value);
						break;
					case VARCHAR:
						ps.setString(index, (String)value);
						break;
					default: throw new IllegalStateException("jdbctype " + nd.getJdbcType() + " is not yet supported");
				}
				break;
			case BOOLEAN:
				ps.setBoolean(index, (Boolean)value);
				break;
			case SQL_DATE: {
				java.sql.Date date = (java.sql.Date)value;
			      switch(jdbcType) {
                      case DATE:
                          ps.setDate(index, date);
                          break;
                      default: throw new IllegalStateException("Cannot convert long to " + jdbcType);
			      }
				break;
			}
			default: throw new IllegalStateException("Parameter not set for " + nd.getName() + " and value " + value);
		}
	}

	private void setNull(final PreparedStatement ps, final int index, final JdbcType type) throws SQLException {
		ps.setNull(index, toSqlTypes(type));
	}

	private int toSqlTypes(JdbcType type) {
		switch(type) {
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
