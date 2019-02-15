package scott.barleydb.api.graphql;

import java.util.function.Predicate;

import scott.barleydb.api.query.QJoin;

/**
 * Customizes the query execution of graphql
 * @author scott
 *
 */
public class GraphQLQueryCustomizations {
	
	private Predicate<QJoin> shouldBreakPredicate;

	public boolean shouldBreakJoin(QJoin join) {
		if (shouldBreakPredicate == null) {
			return false;
		}
		return shouldBreakPredicate.test(join);
	}

	public void setShouldBreakPredicate(Predicate<QJoin> shouldBreakPredicate) {
		this.shouldBreakPredicate = shouldBreakPredicate;
	}

	public GraphQLQueryCustomizations copy() {
		return new GraphQLQueryCustomizations();
	}
	
	
}
