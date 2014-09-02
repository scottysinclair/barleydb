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
		if (nodeDef.getEnumType() != null) {
		  if (nodeDef.getJdbcType() == JdbcType.INT) {
		      Enum<? extends Enum<?>> enumValue = (Enum<? extends Enum<?>>)qpc.getValue();
		      if (enumValue != null) {
    		      params.add(new QueryGenerator.Param(nodeDef, enumValue.ordinal()));
		      }
		      else {
		          params.add(new QueryGenerator.Param(nodeDef, null));
		      }
		      sb.append('?');
		  }
		  else if (JdbcType.isStringType( nodeDef.getJdbcType() )) {
		      if (qpc.getValue() != null) {
		          params.add(new QueryGenerator.Param(nodeDef, qpc.getValue().toString()));
		      }
		      else {
		          params.add(new QueryGenerator.Param(nodeDef, null));
		      }
		      sb.append('?');
		  }
		  else {
		      throw new IllegalStateException("Enums with jdbc type " + nodeDef.getJdbcType() + " is not supported");
		  }
		}
		else {
            params.add(new QueryGenerator.Param(nodeDef, qpc.getValue()));
            sb.append('?');
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
		QueryGenerator qGen = new QueryGenerator(exists.getSubQueryObject(), definitions);
		sb.append( qGen.generateSQL(params) );
		sb.append(")");
	}
}
