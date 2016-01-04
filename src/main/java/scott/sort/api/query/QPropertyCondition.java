package scott.sort.api.query;

import scott.sort.api.exception.execution.query.IllegalQueryStateException;

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


/**
 * A condition on a model property used to filter results.
 * @author sinclair
 *
 */
public class QPropertyCondition extends QCondition {
    private static final long serialVersionUID = 1L;
    private final QProperty<?> property;
    private final QMathOps operator;
    private final Object value;

    public QPropertyCondition(QProperty<?> property, QMathOps operator, Object value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public QProperty<?> getProperty() {
        return property;
    }

    public QMathOps getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void visit(ConditionVisitor visitor) throws IllegalQueryStateException {
        visitor.visitPropertyCondition(this);
    }

    @Override
    public String toString() {
        return "QPropertyCondition [property=" + property + ", operator="
                + operator + ", value=" + value + "]";
    }
}
