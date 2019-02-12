package scott.barleydb.api.query;

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
import java.util.Set;

import scott.barleydb.api.exception.BarleyDBRuntimeException;

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

    public QPropertyCondition equalsParam(QParameter<VAL> parameter) {
        return new QPropertyCondition(this, QMathOps.EQ, parameter);
    }

    public QPropertyCondition notEqual(VAL value) {
        return new QPropertyCondition(this, QMathOps.NOT_EQ, value);
    }

    public QPropertyCondition like(VAL value) {
        return new QPropertyCondition(this, QMathOps.LIKE, value);
    }

    public QPropertyCondition greaterOrEqual(VAL value) {
        return new QPropertyCondition(this, QMathOps.GREATER_THAN_OR_EQUAL, value);
    }

    public QPropertyCondition greaterOrEqualParam(QParameter<VAL> parameter) {
        return new QPropertyCondition(this, QMathOps.GREATER_THAN_OR_EQUAL, parameter);
    }

    public QPropertyCondition greater(VAL value) {
        return new QPropertyCondition(this, QMathOps.GREATER_THAN, value);
    }

    public QPropertyCondition less(VAL value) {
        return new QPropertyCondition(this, QMathOps.LESS_THAN, value);
    }

    public QPropertyCondition lessParam(QParameter<VAL> parameter) {
        return new QPropertyCondition(this, QMathOps.LESS_THAN, parameter);
    }

    public QPropertyCondition lessOrEqual(VAL value) {
        return new QPropertyCondition(this, QMathOps.LESS_THAN_OR_EQUAL, value);
    }

    public QPropertyCondition isNull() {
      return new QPropertyCondition(this, QMathOps.IS_NULL, null);
   }

    public QPropertyCondition isNotNull() {
      return new QPropertyCondition(this, QMathOps.IS_NOT_NULL, null);
   }

    @Override
    public String toString() {
        return "QProperty [queryObject=" + queryObject + ", propertyDef=" + propertyDef + "]";
    }

    public QPropertyCondition in(Set<VAL> values) {
      if (values.isEmpty()) {
        throw new BarleyDBRuntimeException("Cannot use empty set with IN condition");
      }
      return new QPropertyCondition(this, QMathOps.IN, values);
    }

    public QPropertyCondition notIn(Set<VAL> values) {
      if (values.isEmpty()) {
        throw new BarleyDBRuntimeException("Cannot use empty set with IN condition");
      }
      return new QPropertyCondition(this, QMathOps.NOT_IN, values);
    }

}
