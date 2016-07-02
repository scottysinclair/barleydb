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

import static scott.barleydb.server.jdbc.query.QueryGenerator.getQueryDepth;
import static scott.barleydb.server.jdbc.query.QueryGenerator.toSpaces;

import java.util.List;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.ConditionVisitor;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QExists;
import scott.barleydb.api.query.QLogicalOp;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.server.jdbc.query.QueryGenerator.Param;
import scott.barleydb.server.jdbc.vendor.Database;
import scott.barleydb.server.jdbc.query.QueryGenerator;

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
        NodeType nodeType = et.getNodeType(qpc.getProperty().getName(), true);
        sb.append(qpc.getProperty().getQueryObject().getAlias() + "." + nodeType.getColumnName());
        switch (qpc.getOperator()) {
        case EQ: {
            sb.append(" = ");
            break;
        }
        case NOT_EQ: {
            sb.append(" != ");
            break;
        }
        case LIKE: {
            sb.append(" like ");
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
        params.add(new QueryGenerator.Param(nodeType, qpc.getValue()));
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
