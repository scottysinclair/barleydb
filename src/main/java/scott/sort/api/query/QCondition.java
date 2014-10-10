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

import scott.sort.api.exception.query.IllegalQueryStateException;

/**
 * Base class for query conditions.
 * Contains and and or operations to build larger expressions on top of this one.
 *
 * @author sinclair
 *
 */
public abstract class QCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    public QCondition and(QCondition cond) {
        return new QLogicalOp(this, cond, QBooleanOps.AND);
    }

    public QCondition or(QCondition cond) {
        return new QLogicalOp(this, cond, QBooleanOps.OR);
    }

    public QCondition orExists(QueryObject<?> queryObject) {
        return new QLogicalOp(this, new QExists(queryObject), QBooleanOps.OR);
    }

    public QCondition andExists(QueryObject<?> queryObject) {
        return new QLogicalOp(this, new QExists(queryObject), QBooleanOps.AND);
    }

    public abstract void visit(ConditionVisitor visitor) throws IllegalQueryStateException;
}