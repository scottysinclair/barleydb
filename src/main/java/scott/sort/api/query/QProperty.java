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

public class QProperty<VAL> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final QueryObject<?> queryObject;
    private final String propertyDef;

    public QProperty(QueryObject<?> queryObject, String propertyDef) {
        this.queryObject = queryObject;
        this.propertyDef = propertyDef;
    }

    public QueryObject<?> getQueryObject() {
        return queryObject;
    }

    public String getName() {
        return propertyDef;
    }

    public QPropertyCondition equal(VAL value) {
        return new QPropertyCondition(this, QMathOps.EQ, value);
    }

    public QPropertyCondition greaterOrEqual(VAL value) {
        return new QPropertyCondition(this, QMathOps.GREATER_THAN_OR_EQUAL, value);
    }

    public QPropertyCondition greater(VAL value) {
        return new QPropertyCondition(this, QMathOps.GREATER_THAN, value);
    }

    public QPropertyCondition lessOrEqual(VAL value) {
        return new QPropertyCondition(this, QMathOps.LESS_THAN_OR_EQUAL, value);
    }

    @Override
    public String toString() {
        return "QProperty [queryObject=" + queryObject + ", propertyDef=" + propertyDef + "]";
    }

}