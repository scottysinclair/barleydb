package com.smartstream.sort.server.jdbc.queryexecution;

import java.util.List;

import com.smartstream.sort.api.config.Definitions;
import com.smartstream.sort.api.config.EntityType;
import com.smartstream.sort.api.config.NodeDefinition;
import com.smartstream.sort.api.core.types.*;
import com.smartstream.sort.api.query.ConditionVisitor;
import com.smartstream.sort.api.query.QExists;
import com.smartstream.sort.api.query.QLogicalOp;
import com.smartstream.sort.api.query.QPropertyCondition;
import com.smartstream.sort.server.jdbc.queryexecution.QueryGenerator.Param;

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
        EntityType et = definitions.getEntityTypeMatchingInterface(qpc.getProperty().getQueryObject().getTypeName(), true);
        NodeDefinition nodeDef = et.getNode(qpc.getProperty().getName(), true);
        sb.append(qpc.getProperty().getQueryObject().getAlias() + "." + nodeDef.getColumnName());
        switch (qpc.getOperator()) {
        case EQ: {
            sb.append(" = ");
            break;
        }
        case GREATER_THAN: {
            sb.append(" > ");
            break;
        }
        case GREATER_THAN_OR_EQUAL: {
            sb.append(" >= ");
            break;
        }
        case LESS_THAN: {
            sb.append(" < ");
            break;
        }
        case LESS_THAN_OR_EQUAL: {
            sb.append(" <= ");
            break;
        }
        default:
            throw new IllegalStateException("Unexpected operator");
        }
        params.add(new QueryGenerator.Param(nodeDef, qpc.getValue()));
        sb.append('?');
    }

    public void visitLogicalOp(QLogicalOp qlo) {
        sb.append('(');
        depth++;
        qlo.getLeft().visit(this);
        depth--;
        sb.append(')');

        sb.append(depth == 0 ? '\n' : ' ');
        switch (qlo.getExpr()) {
        case AND: {
            sb.append("AND ");
            break;
        }
        case OR: {
            sb.append("OR ");
            break;
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
        sb.append(qGen.generateSQL(params));
        sb.append(")");
    }
}
