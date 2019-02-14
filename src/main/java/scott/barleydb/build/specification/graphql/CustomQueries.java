package scott.barleydb.build.specification.graphql;

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
	
	/**
	 * 
	 * @param name
	 * @return a new instance of the given query.
	 */
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
