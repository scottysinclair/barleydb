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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.ProjectionColumn;

public class Projection implements Iterable<ProjectionColumn> {

    private final Definitions definitions;
    private List<ProjectionColumn> columns = new LinkedList<>();

    public Projection(Definitions definitions) {
        this.definitions = definitions;
    }

    @Override
    public Iterator<ProjectionColumn> iterator() {
        return columns.iterator();
    }

    public void build(QueryObject<?> query) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeName(), true);
        QJoin qj = query.getJoined();

        /*
         * add all table columns defined by the EntityType for this QueryObject
         */
        for (NodeType nd : entityType.getNodeTypes()) {
            if (nd.getColumnName() != null) {
                ProjectionColumn pCol = new ProjectionColumn(this, query, qj, nd);
                if (requiredInProjection(query, nd)) {
                    columns.add(pCol);
                }
            }
        }
        /*
         * add the table columns for the other query objects which we join to
         */
        for (QJoin join : query.getJoins()) {
            build(join.getTo());
        }
    }

    int indexOf(ProjectionColumn column) throws IllegalQueryStateException {
        int i = columns.indexOf(column);
        if (i == -1) {
            throw new IllegalQueryStateException("Projection column not found in projection: " + column);
        }
        return i + 1; //resultset style 1-N index
    }

    private boolean requiredInProjection(QueryObject<?> query, NodeType nd) {
        if (nd.isPrimaryKey()) {
            /*
             * We always project primary keys
             */
            return true;
        }
        if (nd.isOptimisticLock()) {
            /*
             * We always project optimistic locks
             * Persisting an entity with a lazily loaded optimistic lock
             * would be very problematic..
             */
            return true;
        }
        if (nd.getFixedValue() != null) {
            /*
             * We always project fixed values which are used in parent / child relationship analysis
             */
            return true;
        }
        else if (nd.getRelationInterfaceName() != null) {
            /*
             * We are a FK relation and we are joined in the query then we require it.
             */
            for (QJoin join: query.getJoins()) {
                if (join.getFkeyProperty().equals( nd.getName() )) {
                    return true;
                }
            }
        }
        return query.isProjected(nd.getName());
    }

    public List<ProjectionColumn> getColumns() {
        return columns;
    }

    public List<ProjectionColumn> getColumnsFor(QueryObject<?> queryObject) {
        List<ProjectionColumn> result = new LinkedList<>();
        for (ProjectionColumn column : columns) {
            if (column.getQueryObject() == queryObject) {
                result.add(column);
            }
        }
        return result;
    }

}
