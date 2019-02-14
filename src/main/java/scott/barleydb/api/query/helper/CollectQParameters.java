package scott.barleydb.api.query.helper;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.exception.BarleyDBException;
import scott.barleydb.api.exception.BarleyDBRuntimeException;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.ConditionVisitor;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QExists;
import scott.barleydb.api.query.QLogicalOp;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;

public class CollectQParameters {
	
	public static <T> List<QParameter<?>> forQuery(QueryObject<?> query) {
		List<QParameter<?>> result = new LinkedList<>();
		ConditionVisitor visitor = new ConditionVisitor() {
			@Override
			public void visitPropertyCondition(QPropertyCondition qpc) throws IllegalQueryStateException {
				if (qpc.getValue() instanceof QParameter<?>) {
					result.add((QParameter<?>)qpc.getValue());
				}
			}
			
			@Override
			public void visitLogicalOp(QLogicalOp qlo) throws IllegalQueryStateException, ForUpdateNotSupportedException {
				qlo.getLeft().visit(this);
				qlo.getRight().visit(this);
			}
			
			@Override
			public void visitExists(QExists exists) throws IllegalQueryStateException, ForUpdateNotSupportedException {
				exists.getSubQueryObject().getCondition().visit(this);
			}
		};
		QCondition cond = query.getCondition();
		try {
			cond.visit(visitor);
			return result;
		} catch (IllegalQueryStateException x) {
			throw new BarleyDBRuntimeException("Exception collecting QParameters", x);
		} catch (ForUpdateNotSupportedException x) {
			throw new BarleyDBRuntimeException("Exception collecting QParameters", x);
		}
	}
	
	public static <T> QParameter<T> forQuery(QueryObject<?> query, String parameterName) {
		if (query.getCondition() == null) {
			return null;
		}
		List<QParameter<?>> found = new LinkedList<>();
		try {
			query.getCondition().visit(new ConditionVisitor() {
				
				@Override
				public void visitPropertyCondition(QPropertyCondition qpc) throws IllegalQueryStateException {
					if (qpc.getValue() instanceof QParameter) {
						QParameter<Object> p = (QParameter<Object>)qpc.getValue();
						if (p.getName().equals(parameterName)) {
							found.add(p);
						}
					}
				}
				
				@Override
				public void visitLogicalOp(QLogicalOp qlo) throws IllegalQueryStateException, ForUpdateNotSupportedException {
					qlo.getLeft().visit(this);
					qlo.getRight().visit(this);
				}
				
				@Override
				public void visitExists(QExists exists) throws IllegalQueryStateException, ForUpdateNotSupportedException {
					exists.getSubQueryObject().getCondition().visit(this);
				}
			});
		} catch (IllegalQueryStateException x) {
			throw new IllegalStateException("error processing query", x);
		} catch (ForUpdateNotSupportedException x) {
			throw new IllegalStateException("select for update not supported", x);
		}
		if (found.size() > 1) {
			throw new IllegalStateException("oops");
		}
		if (found.isEmpty()) {
			return null;
		}
		return (QParameter<T>)found.get(0);
	}
}
