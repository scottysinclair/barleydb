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

import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;

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

    public abstract void visit(ConditionVisitor visitor) throws IllegalQueryStateException, ForUpdateNotSupportedException;
}
