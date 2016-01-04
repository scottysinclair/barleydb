package scott.sort.api.query;

import scott.sort.api.exception.execution.query.ForUpdateNotSupportedException;
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
 * query condition which checks for the existence of sub-query results.
 * @author sinclair
 *
 */
public class QExists extends QCondition {

    private static final long serialVersionUID = 1L;
    private final QueryObject<?> subQueryObject;

    public QExists(QueryObject<?> subQueryObject) {
        this.subQueryObject = subQueryObject;
    }

    public QueryObject<?> getSubQueryObject() {
        return subQueryObject;
    }

    @Override
    public void visit(ConditionVisitor visitor) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        visitor.visitExists(this);
    }

}
