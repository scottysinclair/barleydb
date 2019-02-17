package scott.barleydb.api.graphql;

import java.util.function.BiPredicate;

import scott.barleydb.api.query.QJoin;

/**
 * Customizes the query execution of graphql
 * @author scott
 *
 */
public class GraphQLQueryCustomizations {
	
	private BiPredicate<QJoin, GraphQLContext> shouldBreakPredicate;

	public boolean shouldBreakJoin(QJoin join, GraphQLContext graphCtx) {
		if (shouldBreakPredicate == null) {
			return false;
		}
		return shouldBreakPredicate.test(join, graphCtx);
	}

	public void setShouldBreakPredicate(BiPredicate<QJoin, GraphQLContext> shouldBreakPredicate) {
		this.shouldBreakPredicate = shouldBreakPredicate;
	}

	public GraphQLQueryCustomizations copy() {
		return new GraphQLQueryCustomizations();
	}
	
	
}
