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

import java.util.List;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QOrderBy;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.vendor.Database;

public class QueryGenerator {

    /**
     * Prepared statement parameter
     * @author scott
     *
     */
    public static class Param {
        private final NodeType nodeType;
        private final Object value;

        public Param(NodeType nodeType, Object value) {
            this.nodeType = nodeType;
            this.value = (value instanceof QParameter<?>) ? ((QParameter<?>)value).getValue() : value;
        }

        public NodeType getNodeType() {
            return nodeType;
        }

        public Object getValue() {
            return value;
        }
    }

    private final Database database;
    private final QueryObject<?> query;
    private final Definitions definitions;
    private final String initialIndent;

    public QueryGenerator(Database database, QueryObject<?> query, Definitions definitions) {
        this.database = database;
        this.query = query;
        this.definitions = definitions;
        this.initialIndent = toSpaces( getQueryDepth(query) );
    }

    public String generateSQL(Projection projection, List<Param> params) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeName(), true);
        StringBuilder sb = new StringBuilder();

        appendProjection(sb, projection);
        if (projection != null) {
            sb.append('\n');
            sb.append(initialIndent);
        }
        else {
            sb.append(' ');
        }
        sb.append("from ");
        sb.append(database.formatTableName(entityType.getTableName()));
        sb.append(" ");
        sb.append(query.getAlias());
        boolean hasInnerJoins = generateInnerJoinTableDeclarations(sb, query);
        generateLeftOuterJoins(sb, query);
        if (hasInnerJoins) {
            sb.append('\n');
            sb.append(initialIndent);
            sb.append("where");
            generateInnerJoinConditions(sb, query);
        }

        if (query.isSubQuery()) {
            if (hasInnerJoins) {
                sb.append('\n');
                sb.append(initialIndent);
                sb.append("AND ");
            }
            else {
                sb.append(" where ");
            }
            generateSubQueryCondition(sb, query);
            if (query.getCondition() != null) {
                sb.append(" AND ");
            	sb.append(" (");
                query.getCondition().visit(new ConditionRenderer(database, sb, definitions, params, initialIndent));
            	sb.append(" )");
            }
        }
        else {
            if (query.getCondition() != null) {
                if (hasInnerJoins) {
                    sb.append('\n');
                    sb.append(initialIndent);
                    sb.append("AND ");
                }
                else {
                    sb.append('\n');
                    sb.append(initialIndent);
                    sb.append("where ");
                }
                query.getCondition().visit(new ConditionRenderer(database, sb, definitions, params, initialIndent));
            }
            if (!query.getOrderBy().isEmpty()) {
                generateOrderBy(sb);
            }
            if (query.getForUpdate() != null) {
                generateForUpdate(sb);
            }
        }
        return sb.toString();
    }

    private boolean generateInnerJoinTableDeclarations(StringBuilder sb, QueryObject<?> queryObject) {
        boolean foundOne = false;
        for (QJoin qj: queryObject.getJoins()) {
            if (qj.getJoinType() == JoinType.INNER) {
                foundOne = true;
                sb.append(", ");
                EntityType entityType = definitions.getEntityTypeMatchingInterface(qj.getTo().getTypeName(), true);
                sb.append(database.formatTableName(entityType.getTableName()));
                sb.append(' ');
                sb.append(qj.getTo().getAlias());
            }
            generateInnerJoinTableDeclarations(sb, qj.getTo());
        }
        return foundOne;
    }

    private void generateLeftOuterJoins(StringBuilder sb, QueryObject<?> queryObject) throws IllegalQueryStateException {
        for (QJoin qj: queryObject.getJoins()) {
            switch(qj.getJoinType()) {
                /*
                 * We found a left outer join so generate it
                 */
                case LEFT_OUTER: generateLeftOuterJoin(sb, qj);
                    break;
                /*
                 * we found an inner join, so check if it's target has any left outer joins.
                 */
                case INNER: generateLeftOuterJoins(sb, qj.getTo());
                    break;
                default:
                    break;
            }
        }
    }

    private void generateOrderBy(StringBuilder sb) {
        sb.append("\norder by ");
        for (QOrderBy orderby : query.getOrderBy()) {
            QueryObject<?> qo = orderby.getProperty().getQueryObject();
            EntityType entityType = definitions.getEntityTypeMatchingInterface(qo.getTypeName(), true);
            sb.append(qo.getAlias());
            sb.append('.');
            sb.append(entityType.getNodeType(orderby.getProperty().getName(), true).getColumnName());
            if (orderby.isAscending()) {
                sb.append(" asc");
            }
            else {
                sb.append(" desc");
            }
            sb.append(',');
        }
        sb.setLength(sb.length() - 1);
    }

    private void generateForUpdate(StringBuilder sb) throws ForUpdateNotSupportedException {
        if (!database.supportsSelectForUpdate()) {
            throw new ForUpdateNotSupportedException("For update not supported by " + database.getInfo());
        }
        sb.append(" for update");
        if (query.getForUpdate().getOptionalWaitInSeconds() != null) {
            if (!database.supportsSelectForUpdateWaitN()) {
                throw new ForUpdateNotSupportedException("For update wait <seconds> not supported by " + database.getInfo());
            }
            sb.append(" wait " + query.getForUpdate().getOptionalWaitInSeconds());
        }
    }

    public String generateSQL(List<Param> params) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        return generateSQL(null, params);
    }

    private void generateSubQueryCondition(StringBuilder sb, QueryObject<?> query) {
        QJoin qjoin = query.getSubQueryJoin();
        //e.g. syntax
        EntityType entityFrom = definitions.getEntityTypeMatchingInterface(qjoin.getFrom().getTypeName(), true);
        //e.g. xmlmappings
        EntityType entityTo = definitions.getEntityTypeMatchingInterface(qjoin.getTo().getTypeName(), true);

        //for example 'syntax' node in the XMLMappings entity, when doing a sub-query from syntax to mappings
        String nodeNameInTo = entityFrom.getNodeType(qjoin.getFkeyProperty(), true).getForeignNodeName();
        if (nodeNameInTo != null) {
            sb.append(qjoin.getTo().getAlias() + "." + entityTo.getNodeType(nodeNameInTo, true).getColumnName());
            sb.append(" = ");
            sb.append(qjoin.getFrom().getAlias() + "." + entityFrom.getKeyColumns().get(0));
        }
        else {
            NodeType nodeType = entityFrom.getNodeType(qjoin.getFkeyProperty(), true);
            sb.append(qjoin.getTo().getAlias() + "." + entityTo.getKeyColumns().get(0));
            sb.append(" = ");
            sb.append(qjoin.getFrom().getAlias() + "." + nodeType.getColumnName());
        }
    }

    private void appendProjection(StringBuilder sb, Projection projection) {
        sb.append("select ");
        if (projection != null && !projection.getColumns().isEmpty()) {
            for (ProjectionColumn pc : projection) {
                sb.append(pc.getQueryObject().getAlias());
                sb.append('.');
                sb.append(pc.getColumn());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        else {
            //subquery
            sb.append("1");
        }
    }

    private void generateInnerJoinConditions(StringBuilder sb, QueryObject<?> query) throws IllegalQueryStateException {
        boolean firstCondition = true;
        for (QJoin qj: query.getJoins()) {
            if (qj.getJoinType() == JoinType.INNER) {
                if (!firstCondition) {
                    sb.append('\n');
                    sb.append(initialIndent);
                    sb.append("AND ");
                }
                else  {
                    sb.append(' ');
                }
                firstCondition = false;
                generateInnerJoinCondition(sb, qj);
            }
        }
    }

    private void generateInnerJoinCondition(StringBuilder sb, QJoin join) throws IllegalQueryStateException {
        EntityType entityTo = definitions.getEntityTypeMatchingInterface(join.getTo().getTypeName(), true);
        EntityType entityFrom = definitions.getEntityTypeMatchingInterface(join.getFrom().getTypeName(), true);

        String nodeNameOfForeignKey = entityFrom.getNodeType(join.getFkeyProperty(), true).getForeignNodeName();
        if (nodeNameOfForeignKey != null) {
            String foreignKeyCol = entityTo.getNodeType(nodeNameOfForeignKey, true).getColumnName();
            sb.append(join.getTo().getAlias() + "." + foreignKeyCol);
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getKeyColumn());
        }
        else {
            sb.append(join.getTo().getAlias() + "." + entityTo.getKeyColumn());
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getNodeType(join.getFkeyProperty(), true).getColumnName());
        }
        for (QJoin qj : join.getTo().getJoins()) {
            if (qj.getJoinType() == JoinType.INNER) {
                sb.append(" AND ");
                generateInnerJoinCondition(sb, qj);
            }
        }
    }

    private void generateLeftOuterJoin(StringBuilder sb, QJoin join) throws IllegalQueryStateException {
        EntityType entityTo = definitions.getEntityTypeMatchingInterface(join.getTo().getTypeName(), true);
        EntityType entityFrom = definitions.getEntityTypeMatchingInterface(join.getFrom().getTypeName(), true);

        //nodeNameOfForeignKey, like the syntax node in the mappings entity which maps to the owning syntax.
        String nodeNameOfForeignKey = entityFrom.getNodeType(join.getFkeyProperty(), true).getForeignNodeName();
        if (nodeNameOfForeignKey != null) {
            String foreignKeyCol = entityTo.getNodeType(nodeNameOfForeignKey, true).getColumnName();
            sb.append("\nleft outer join ");
            sb.append(database.formatTableName(entityTo.getTableName()) + " " + join.getTo().getAlias());
            sb.append(" on ");
            sb.append(join.getTo().getAlias() + "." + foreignKeyCol);
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getKeyColumn());
        }
        else {
            //simple 1:1 join
            sb.append("\nleft outer join ");
            sb.append(database.formatTableName(entityTo.getTableName()) + " " + join.getTo().getAlias());
            sb.append(" on ");
            sb.append(join.getTo().getAlias() + "." + entityTo.getKeyColumn());
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getNodeType(join.getFkeyProperty(), true).getColumnName());
        }

        for (QJoin qj : join.getTo().getJoins()) {
            if (qj.getJoinType() != JoinType.LEFT_OUTER) {
                throw new IllegalQueryStateException("A left outer join query cannot itself have an inner join");
            }
            generateLeftOuterJoin(sb, qj);
        }
    }

    static String toSpaces(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<num; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    static int getQueryDepth(QueryObject<?> query) {
       QueryObject<?> parent = query.getParent();
       return parent == null ? 0 : 1 + getQueryDepth(parent);
    }
}
