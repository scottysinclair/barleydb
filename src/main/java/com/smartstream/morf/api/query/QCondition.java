package com.smartstream.morf.api.query;

import java.io.Serializable;

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

    public abstract void visit(ConditionVisitor visitor);
}