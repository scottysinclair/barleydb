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


import scott.sort.api.config.NodeDefinition;
import scott.sort.api.exception.query.IllegalQueryStateException;
import scott.sort.api.query.QJoin;
import scott.sort.api.query.QueryObject;

/**
 * represents a column in the SQL projection
 * @author sinclair
 *
 */
class ProjectionColumn {
    private final Projection projection;
    private final QueryObject<?> queryObject;
    private final QJoin qJoin;
    private final NodeDefinition nodeDefinition;

    public ProjectionColumn(Projection projection, QueryObject<?> queryObject, QJoin qJoin, NodeDefinition nodeDefinition) {
        this.projection = projection;
        this.queryObject = queryObject;
        this.qJoin = qJoin;
        this.nodeDefinition = nodeDefinition;
    }

    public QueryObject<?> getQueryObject() {
        return queryObject;
    }

    public boolean isPrimaryKey() {
        return nodeDefinition.isPrimaryKey();
    }

    public boolean isForeignKey() {
        return nodeDefinition.isForeignKey();
    }

    public boolean isJoined() {
        return qJoin != null;
    }

    public QJoin getQJoin() {
        return qJoin;
    }

    public Integer getIndex() throws IllegalQueryStateException {
        return projection.indexOf(this);
    }

    /**
     * the java bean property to map the data to.
     *
     * @return
     */
    public String getProperty() {
        return nodeDefinition.getName();
    }

    public String getColumn() {
        return nodeDefinition.getColumnName();
    }

    public NodeDefinition getNodeDefinition() {
        return nodeDefinition;
    }

    @Override
    public String toString() {
        try {
            return "ProjectionColumn [ " + getProperty() + "/" + getColumn() + "(" + getIndex() + ")]";
        }
        catch(IllegalQueryStateException x) {
            return "ProjectionColumn [ " + getProperty() + "/" + getColumn() + "( UNKNOWN )]";
        }
    }
}