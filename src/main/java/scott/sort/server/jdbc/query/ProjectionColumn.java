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


import scott.sort.api.config.NodeType;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
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
    private final NodeType nodeType;

    public ProjectionColumn(Projection projection, QueryObject<?> queryObject, QJoin qJoin, NodeType nodeType) {
        this.projection = projection;
        this.queryObject = queryObject;
        this.qJoin = qJoin;
        this.nodeType = nodeType;
    }

    public QueryObject<?> getQueryObject() {
        return queryObject;
    }

    public boolean isPrimaryKey() {
        return nodeType.isPrimaryKey();
    }

    public boolean isForeignKey() {
        return nodeType.isForeignKey();
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
        return nodeType.getName();
    }

    public String getColumn() {
        return nodeType.getColumnName();
    }

    public NodeType getNodeType() {
        return nodeType;
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