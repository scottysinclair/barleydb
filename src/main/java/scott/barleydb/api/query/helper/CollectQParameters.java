package scott.barleydb.api.query.helper;

import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.ConditionVisitor;
import scott.barleydb.api.query.QExists;
import scott.barleydb.api.query.QLogicalOp;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;

public class CollectQParameters {
	
	public static <T> QParameter<T> forQuery(QueryObject<?> query, String parameterName) {
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
