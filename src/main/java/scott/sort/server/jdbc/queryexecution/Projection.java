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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeDefinition;
import scott.sort.api.exception.query.IllegalQueryStateException;
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
        for (NodeDefinition nd : entityType.getNodeDefinitions()) {
            if (nd.getColumnName() != null) {
                ProjectionColumn pCol = new ProjectionColumn(this, query, qj, nd);
                if (nd.isPrimaryKey() || !query.isDisabled(nd.getName())) {
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
