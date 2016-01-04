package scott.sort.api.query;

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


import java.io.Serializable;

/**
 * A join in a query from one query object to another.
 * Contains the from and to side of the join as well as the field which is used in the join.
 *
 * @author sinclair
 *
 */
public class QJoin implements Serializable {

    private static final long serialVersionUID = 1L;
    private final QueryObject<?> from;
    private final QueryObject<?> to;
    private final JoinType joinType;
    private final String fkeyProperty;

    public QJoin(QueryObject<?> from, QueryObject<?> to, String fkeyProperty, JoinType joinType) {
        this.from = from;
        this.to = to;
        this.fkeyProperty = fkeyProperty;
        this.joinType = joinType;
    }

    public QueryObject<?> getFrom() {
        return from;
    }

    public QueryObject<?> getTo() {
        return to;
    }

    public String getFkeyProperty() {
        return fkeyProperty;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public String toString() {
        return "QJoin [from=" + from + ", to=" + to + ", joinType=" + joinType + ", fkeyProperty=" + fkeyProperty + "]";
    }
}
