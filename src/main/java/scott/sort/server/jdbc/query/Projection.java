package scott.sort.server.jdbc.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
import scott.sort.api.query.QJoin;
import scott.sort.api.query.QueryObject;

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
             */
            return true;
        }
        else if (nd.getFixedValue() != null) {
            /*
             * We always project fixed values which are used in parent / child relationship analysis
             */
            return true;
        }
        else if (nd.getRelationInterfaceName() != null) {
            /*
             * We are a FK relation so we always include it
             */
            return true;
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
