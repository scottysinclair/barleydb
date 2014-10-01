package com.smartstream.sort.server.jdbc.queryexecution;

import com.smartstream.sort.api.config.NodeDefinition;
import com.smartstream.sort.api.query.QJoin;
import com.smartstream.sort.api.query.QueryObject;

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

    public Integer getIndex() {
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
        return "ProjectionColumn [ " + getProperty() + "/" + getColumn() + "(" + getIndex() + ")]";
    }
}