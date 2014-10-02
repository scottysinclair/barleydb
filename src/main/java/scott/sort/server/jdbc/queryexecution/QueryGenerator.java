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


import java.util.*;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeDefinition;
import scott.sort.api.query.QJoin;
import scott.sort.api.query.QOrderBy;
import scott.sort.api.query.QueryObject;

public class QueryGenerator {

    /**
     * Prepared statement parameter
     * @author scott
     *
     */
    public static class Param {
        private final NodeDefinition nodeDef;
        private final Object value;

        public Param(NodeDefinition nodeDef, Object value) {
            this.nodeDef = nodeDef;
            this.value = value;
        }

        public NodeDefinition getNodeDefinition() {
            return nodeDef;
        }

        public Object getValue() {
            return value;
        }
    }

    private final QueryObject<?> query;
    private final Definitions definitions;

    public QueryGenerator(QueryObject<?> query, Definitions definitions) {
        this.query = query;
        this.definitions = definitions;
    }

    public String generateSQL(Projection projection, List<Param> params) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeName(), true);

        StringBuilder sb = new StringBuilder();
        appendProjection(sb, projection);
        sb.append(projection != null ? '\n' : ' ');
        sb.append("from ");
        sb.append(entityType.getTableName());
        sb.append(" ");
        sb.append(query.getAlias());
        for (QJoin qj : query.getJoins()) {
            renderJoin(sb, qj);
        }
        if (query.isSubQuery()) {
            sb.append(" where ");
            generateSubQueryCondition(sb, query);
            if (query.getCondition() != null) {
                sb.append(" AND ");
                query.getCondition().visit(new ConditionRenderer(sb, definitions, params));
            }
        }
        else {
            if (query.getCondition() != null) {
                sb.append("\nwhere ");
                query.getCondition().visit(new ConditionRenderer(sb, definitions, params));
            }
            if (!query.getOrderBy().isEmpty()) {
                sb.append("\norder by ");
                for (QOrderBy orderby : query.getOrderBy()) {
                    sb.append(entityType.getNode(orderby.getProperty().getName(), true).getColumnName());
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
        }
        return sb.toString();
    }

    public String generateSQL(List<Param> params) {
        return generateSQL(null, params);
    }

    private void generateSubQueryCondition(StringBuilder sb, QueryObject<?> query) {
        QJoin qjoin = query.getSubQueryJoin();
        //e.g. syntax
        EntityType entityFrom = definitions.getEntityTypeMatchingInterface(qjoin.getFrom().getTypeName(), true);
        //e.g. xmlmappings
        EntityType entityTo = definitions.getEntityTypeMatchingInterface(qjoin.getTo().getTypeName(), true);

        //for example 'syntax' node in the XMLMappings entity, when doing a sub-query from syntax to mappings
        String nodeNameInTo = entityFrom.getNode(qjoin.getFkeyProperty(), true).getForeignNodeName();
        if (nodeNameInTo != null) {
            sb.append(qjoin.getTo().getAlias() + "." + entityTo.getNode(nodeNameInTo, true).getColumnName());
            sb.append(" = ");
            sb.append(qjoin.getFrom().getAlias() + "." + entityFrom.getKeyColumn());
        }
        else {
            NodeDefinition nodeDef = entityFrom.getNode(qjoin.getFkeyProperty(), true);
            sb.append(qjoin.getTo().getAlias() + "." + entityTo.getKeyColumn());
            sb.append(" = ");
            sb.append(qjoin.getFrom().getAlias() + "." + nodeDef.getColumnName());
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
            sb.append("*");
        }
    }

    private void renderJoin(StringBuilder sb, QJoin join) {
        EntityType entityTo = definitions.getEntityTypeMatchingInterface(join.getTo().getTypeName(), true);
        EntityType entityFrom = definitions.getEntityTypeMatchingInterface(join.getFrom().getTypeName(), true);

        //nodeNameOfForeignKey, like the syntax node in the mappings entity which maps to the owning syntax.
        String nodeNameOfForeignKey = entityFrom.getNode(join.getFkeyProperty(), true).getForeignNodeName();
        if (nodeNameOfForeignKey != null) {
            String foreignKeyCol = entityTo.getNode(nodeNameOfForeignKey, true).getColumnName();
            sb.append("\nleft outer join ");
            sb.append(entityTo.getTableName() + " " + join.getTo().getAlias());
            sb.append(" on ");
            sb.append(join.getTo().getAlias() + "." + foreignKeyCol);
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getKeyColumn());
        }
        else {
            sb.append("\nleft outer join ");
            sb.append(entityTo.getTableName() + " " + join.getTo().getAlias());
            sb.append(" on ");
            sb.append(join.getTo().getAlias() + "." + entityTo.getKeyColumn());
            sb.append(" = ");
            sb.append(join.getFrom().getAlias() + "." + entityFrom.getNode(join.getFkeyProperty(), true).getColumnName());
        }

        for (QJoin qj : join.getTo().getJoins()) {
            renderJoin(sb, qj);
        }
    }

}
