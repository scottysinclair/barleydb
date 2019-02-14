package scott.barleydb.api.graphql;

import scott.barleydb.api.query.QJoin;

/**
 * Customizes the query execution of graphql
 * @author scott
 *
 */
public class GraphQLQueryCustomizations {

	public boolean shouldBreakJoin(QJoin join) {
		return true;
	}

	public GraphQLQueryCustomizations copy() {
		return new GraphQLQueryCustomizations();
	}
	
	
}
