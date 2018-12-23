package scott.barleydb.build.specification.graphql;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import scott.barleydb.api.core.QueryRegistry;
import scott.barleydb.api.query.QueryObject;

public class CustomQueries {
	private final Map<String,QueryObject<?>> queries = new LinkedHashMap<>();

	public void register(String name, QueryObject<?> query) {
		queries.put(name, query);
	}
	
	public QueryObject<?> getQuery(String name) {
		QueryObject<?> query = queries.get(name);
		if (query != null) {
			query = QueryRegistry.clone(query);
		}
		return query;
	}
	
	public Set<Map.Entry<String, QueryObject<?>>> queries() {
		return Collections.unmodifiableSet(queries.entrySet());
	}
}
