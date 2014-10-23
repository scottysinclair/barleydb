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

import java.util.List;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeDefinition;
import scott.sort.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
import scott.sort.api.query.ConditionVisitor;
import scott.sort.api.query.QCondition;
import scott.sort.api.query.QExists;
import scott.sort.api.query.QLogicalOp;
import scott.sort.api.query.QPropertyCondition;
import scott.sort.server.jdbc.database.Database;
import scott.sort.server.jdbc.queryexecution.QueryGenerator.Param;
import static scott.sort.server.jdbc.queryexecution.QueryGenerator.*;

public class ConditionRenderer implements ConditionVisitor {
    private final Database database;
    private final StringBuilder sb;
    private final Definitions definitions;
    private List<Param> params;
    @SuppressWarnings("unused")
    private int depth = 0;
    private String initialIndent;

    public ConditionRenderer(Database database, StringBuilder sb, Definitions definitions, List<Param> params, String initialIndent) {
        this.database = database;
        this.sb = sb;
        this.definitions = definitions;
        this.params = params;
        this.initialIndent = initialIndent;
    }

    public void visitPropertyCondition(QPropertyCondition qpc) throws IllegalQueryStateException {
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
            throw new IllegalQueryStateException("Unexpected operator");
        }
        params.add(new QueryGenerator.Param(nodeDef, qpc.getValue()));
        sb.append('?');
    }

    private boolean parensRequired(QLogicalOp parent, QCondition child) {
        if (!(child instanceof QLogicalOp)) {
            return false;
        }
        QLogicalOp lChild = (QLogicalOp)child;
        return !lChild.getExpr().equals( parent.getExpr() );
    }

    public void visitLogicalOp(QLogicalOp qlo) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        if (!parensRequired(qlo, qlo.getLeft())) {
            //same operator to parenthesis required
            depth++;
            qlo.getLeft().visit(this);
            depth--;
        }
        else {
            sb.append('(');
            depth++;
            qlo.getLeft().visit(this);
            depth--;
            sb.append(')');
        }

        if (!Character.isWhitespace(sb.charAt(sb.length()-1))) {
            sb.append(' ');
        }
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
        if (!parensRequired(qlo, qlo.getRight())) {
            //same operator to parenthesis required
            depth++;
            qlo.getRight().visit(this);
            depth--;
        }
        else {
            sb.append('(');
            depth++;
            qlo.getRight().visit(this);
            depth--;
            sb.append(')');
        }
    }

    public void visitExists(QExists exists) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        sb.append("exists (\n");
        sb.append( toSpaces( getQueryDepth( exists.getSubQueryObject() )  ) );
        QueryGenerator qGen = new QueryGenerator(database, exists.getSubQueryObject(), definitions);
        sb.append(qGen.generateSQL(params));
        sb.append('\n');
        sb.append( initialIndent );
        sb.append(')');
        sb.append('\n');
        sb.append( initialIndent );
    }
}
