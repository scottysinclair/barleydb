package com.smartstream.morf.server.jdbc.queryexecution;


import java.util.List;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.config.NodeDefinition;
import com.smartstream.morf.api.core.types.*;
import com.smartstream.morf.api.query.ConditionVisitor;
import com.smartstream.morf.api.query.QExists;
import com.smartstream.morf.api.query.QLogicalOp;
import com.smartstream.morf.api.query.QPropertyCondition;
import com.smartstream.morf.server.jdbc.queryexecution.QueryGenerator.Param;

public class ConditionRenderer implements ConditionVisitor {
	private final StringBuilder sb;
	private final Definitions definitions;
	private int depth;
	private List<Param> params;

	public ConditionRenderer(StringBuilder sb, Definitions definitions, List<Param> params) {
		this.sb = sb;
		this.definitions = definitions;
		this.params = params;
		depth = 0;
	}

	@SuppressWarnings("unchecked")
    public void visitPropertyCondition(QPropertyCondition qpc) {
		EntityType et = definitions.getEntityTypeMatchingInterface(qpc.getProperty().getQueryObject().getTypeName(), true );
		NodeDefinition nodeDef = et.getNode( qpc.getProperty().getName(), true );
		sb.append(qpc.getProperty().getQueryObject().getAlias() + "." + nodeDef.getColumnName());
		switch(qpc.getOperator()) {
			case EQ: {
				sb.append(" = "); break;
			}
			case GREATER_THAN: {
				sb.append(" > "); break;
			}
			case GREATER_THAN_OR_EQUAL: {
				sb.append(" >= "); break;
			}
			case LESS_THAN: {
				sb.append(" < "); break;
			}
			case LESS_THAN_OR_EQUAL: {
				sb.append(" <= "); break;
			}
			default: throw new IllegalStateException("Unexpected operator");
		}
		if (JavaType.STRING.equals(nodeDef.getJavaType())) {
		    sb.append('\'');
		    sb.append( String.valueOf(qpc.getValue()).replaceAll("'", "''") );
		    sb.append('\'');
		}
		else if (JdbcType.TIMESTAMP.equals(nodeDef.getJdbcType()) || JdbcType.DATE.equals(nodeDef.getJdbcType())) {
		  params.add(new QueryGenerator.Param(nodeDef, qpc.getValue()));
		  sb.append('?');
		}
		else if (nodeDef.getEnumType() != null) {
		  if (nodeDef.getJdbcType() == JdbcType.INT) {
		      sb.append(((Enum<? extends Enum<?>>)qpc.getValue()).ordinal());
		  }
		  else if (JdbcType.isStringType( nodeDef.getJdbcType() )) {
		      sb.append(qpc.getValue().toString());
		  }
		  else {
		      throw new IllegalStateException("Enums with jdbc type " + nodeDef.getJdbcType() + " is not supported");
		  }
		}
		else {
			sb.append( qpc.getValue() );
		}
	}

	public void visitLogicalOp(QLogicalOp qlo) {
		sb.append('(');
		depth++;
		qlo.getLeft().visit(this);
		depth--;
		sb.append(')');

		sb.append(depth == 0 ? '\n' : ' ');
		switch(qlo.getExpr()) {
			case AND: {
				sb.append("AND "); break;
			}
			case OR: {
				sb.append("OR "); break;
			}
		}
		sb.append('(');
		depth++;
//		LOG.debug(qlo);
		qlo.getRight().visit(this);
		depth--;
		sb.append(')');
	}

	public void visitExists(QExists exists) {
		sb.append("exists (");
		QueryGenerator qGen = new QueryGenerator(exists.getSubQueryObject(), definitions, params);
		sb.append( qGen.generateSQL() );
		sb.append(")");
	}
}
