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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.exception.model.QPropertyInvalidException;
import scott.sort.api.exception.model.QPropertyMissingException;
import scott.sort.api.exception.model.SortQueryModelRuntimeException;

/**
 *
 * The scott.sort.definitions abstraction for querying.
 * The query object is the entry point for an abstract model based query API.
 *
 * The query API is in no way specific to JDBC.
 *
 * @author scott
 *
 * @param <R>
 */
public class QueryObject<R> implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(QueryObject.class);

    private final Class<R> typeClass;
    private final String typeName;
    private final QueryObject<?> parent;

    /**
     * Allows QProperty objects to be looked up based on name.
     */
    private final QPropertyLookup propertyLookup;

    /**
     * The set of entity properties returned by the query
     * if empty then all properties will be returned.
     */
    private final Set<String> projectedProperties;
    private final List<QJoin> joins;
    private final List<QJoin> exists;
    private String alias;
    private QAliasGenerator aliasGenerator;
    private QJoin joined;
    private QCondition condition; //the user condition
    private List<QOrderBy> orderBy;
    /**
     * Select .. for update
     */
    private QForUpdate forUpdate;

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
        this.projectedProperties = new HashSet<>();
        this.joins = new LinkedList<QJoin>();
        this.exists = new LinkedList<QJoin>();
        this.orderBy = new LinkedList<QOrderBy>();
        try {
            /*
             * Letting 'this' escape is ok, QPropertyLookup only looks at the static class info.
             */
            this.propertyLookup = new QPropertyLookup(this);
        } catch (QPropertyInvalidException x) {
            /*
             * We wrap the checked exception with a runtime exception as this will
             * a) rarely occur and the client programmer should not always have to check for it
             * b) will be throw when setting up the definitions at startup so the client programmer code would not be reached anyway.
             */
            throw new SortQueryModelRuntimeException("Class " + getClass().getName() + " has an invalid query property", x);
        }
    }

    /**
     * The select clause defines what columns are selected for.<br/>
     *<br/>
     * note: the underlying query executer may decide to select more columns than specified, if required.<br/>
     * Examples would be primary keys and optimistic lock values.
     *
     * @param properties
     * @return
     */
    public QueryObject<R> select(QProperty<?> ...properties) {
        /**
         * Configure each query object according to the properties.
         */
        for (QProperty<?> property: properties) {
            property.getQueryObject().projectedProperties.clear();
        }
        for (QProperty<?> property: properties) {
            property.getQueryObject().projectedProperties.add(property.getName());
        }
        return this;
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

    public boolean isProjected(String propertyName) {
        if (projectedProperties.isEmpty()) {
            return true;
        }
        else {
            return projectedProperties.contains(propertyName);
        }
    }

    public void addInnerJoin(QueryObject<?> to, String propertyDef) {
        addJoin(to, propertyDef, JoinType.INNER);
    }

    public void addLeftOuterJoin(QueryObject<?> to, String propertyDef) {
        addJoin(to, propertyDef, JoinType.LEFT_OUTER);
    }

    public void addJoin(QueryObject<?> to, String propertyDef, JoinType joinType) {
        QJoin qj = new QJoin(this, to, propertyDef, joinType);
        to.joined = qj;
        to.aliasGenerator = getAliasGenerator();
        joins.add(qj);
    }

    public void addExists(QueryObject<?> to, String propertyDef) {
        QJoin qe = new QJoin(this, to, propertyDef, JoinType.EXISTS);
        to.aliasGenerator = getAliasGenerator();
        exists.add(qe);
    }

    public List<QJoin> getJoins() {
        return joins;
    }

    public QJoin getJoined() {
        return joined;
    }

    /**
     * Gets the join used to join this subquery to
     * the parent query.
     * @return
     */
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

    public QueryObject<R> forUpdate() {
        forUpdate = new QForUpdate(null);
        return this;
    }

    public QueryObject<R> forUpdateWait(int seconds) {
        forUpdate = new QForUpdate( seconds );
        return this;
    }

    public QForUpdate getForUpdate() {
        return forUpdate;
    }

    public List<QOrderBy> getOrderBy() {
        return orderBy;
    }

    public QCondition getCondition() {
        return condition;
    }

    /**
     *
     * @return the parent query if this instance is a sub query.
     */
    public QueryObject<?> getParent() {
        return parent;
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

    public QProperty<?> getMandatoryQProperty(String propertyName) throws QPropertyMissingException, QPropertyInvalidException {
        return propertyLookup.getProperty(propertyName);
    }

    private QExists exists(QueryObject<?> queryObject) {
        return new QExists(queryObject);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ";
    }

}
