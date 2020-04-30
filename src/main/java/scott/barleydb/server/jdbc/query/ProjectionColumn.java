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


import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.Projection;

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
