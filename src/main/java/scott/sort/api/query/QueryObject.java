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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryObject<R> implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(QueryObject.class);

    private final Class<R> typeClass;
    private final String typeName;
    private final QueryObject<?> parent;
    private final Set<String> disabled;
    private final List<QJoin> joins;
    private final List<QJoin> exists;
    private String alias;
    private QAliasGenerator aliasGenerator;
    private QJoin joined;
    private QCondition condition; //the user condition
    private List<QOrderBy> orderBy;

    public QueryObject(Class<R> type) {
        this(type, type.getName(), null);
    }

    public QueryObject(Class<R> type, QueryObject<?> parent) {
        this(type, type.getName(), parent);
    }

    public QueryObject(Class<R> typeClass, String typeName, QueryObject<?> parent) {
        this.typeClass = typeClass;
        this.typeName = typeName;
        this.parent = parent;
        this.disabled = new HashSet<>();
        this.joins = new LinkedList<QJoin>();
        this.exists = new LinkedList<QJoin>();
        this.orderBy = new LinkedList<QOrderBy>();
    }

    @SuppressWarnings("unchecked")
    public <T> T back(Class<T> type) {
        return (T) joined.getFrom();
    }

    public String getTypeName() {
        return typeName;
    }

    public Class<R> getTypeClass() {
        return typeClass;
    }

    public boolean isDisabled(String propertyName) {
        return disabled.contains(propertyName);
    }

    public void addDisabled(String propertyDef) {
        disabled.add(propertyDef);
    }

    public void addJoin(QueryObject<?> to, String propertyDef) {
        QJoin qj = new QJoin(this, to, propertyDef);
        to.joined = qj;
        to.aliasGenerator = getAliasGenerator();
        joins.add(qj);
    }

    public void addExists(QueryObject<?> to, String propertyDef) {
        QJoin qe = new QJoin(this, to, propertyDef);
        to.aliasGenerator = getAliasGenerator();
        exists.add(qe);
    }

    public List<QJoin> getJoins() {
        return joins;
    }

    public QJoin getJoined() {
        return joined;
    }

    public QJoin getSubQueryJoin() {
        if (parent != null) {
            for (QJoin join : parent.exists) {
                if (join.getTo() == this) {
                    return join;
                }
            }
        }
        return null;
    }

    public QueryObject<R> where(QCondition condition) {
        this.condition = condition;
        return this;
    }

    public QueryObject<R> whereExists(QueryObject<?> subQuery) {
        this.condition = new QExists(subQuery);
        return this;
    }

    public QueryObject<R> and(QCondition condition) {
        if (this.condition == null) {
            this.condition = condition;
        }
        this.condition = new QLogicalOp(this.condition, condition, QBooleanOps.AND);
        return this;
    }

    public QueryObject<R> or(QCondition condition) {
        if (this.condition == null) {
            this.condition = condition;
        }
        this.condition = new QLogicalOp(this.condition, condition, QBooleanOps.OR);
        return this;
    }

    public QueryObject<R> orderBy(QProperty<?> property, boolean ascending) {
        this.orderBy.add(new QOrderBy(property, ascending));
        return this;
    }

    public List<QOrderBy> getOrderBy() {
        return orderBy;
    }

    public QCondition getCondition() {
        return condition;
    }

    public boolean isSubQuery() {
        return parent != null;
    }

    public String getAlias() {
        if (alias == null) {
            alias = getAliasGenerator().nextAlias();
        }
        return alias;
    }

    private QAliasGenerator getAliasGenerator() {
        if (aliasGenerator == null) {
            aliasGenerator = new QAliasGenerator();
        }
        return aliasGenerator;
    }

    public QueryObject<R> andExists(QueryObject<?> queryObject) {
        this.condition = new QLogicalOp(this.condition, exists(queryObject), QBooleanOps.AND);
        return this;
    }

    public QueryObject<R> orExists(QueryObject<?> queryObject) {
        this.condition = new QLogicalOp(this.condition, exists(queryObject), QBooleanOps.OR);
        return this;
    }

    private QExists exists(QueryObject<?> queryObject) {
        return new QExists(queryObject);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ";
    }

}
