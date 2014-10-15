package scott.sort.api.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
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