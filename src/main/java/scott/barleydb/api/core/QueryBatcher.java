package scott.barleydb.api.core;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.api.core.QueryBatcher;

public class QueryBatcher implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<QueryObject<?>> queries = new LinkedList<>();
    private List<QueryResult<?>> results = new LinkedList<>();

    public void addQuery(QueryObject<?>... queryObjects) {
        for (QueryObject<?> qo : queryObjects) {
            queries.add(qo);
        }
    }

    public List<QueryResult<?>> getResults() {
        return results;
    }

    @SuppressWarnings("unchecked")
    public <T> QueryResult<T> getResult(int index, Class<T> type) {
        return (QueryResult<T>) results.get(index);
    }

    public int size() {
        return queries.size();
    }

    public Collection<QueryObject<?>> getQueries() {
        return queries;
    }

    public void addResult(QueryResult<?> result) {
        results.add(result);
    }

    /**
     * Copies the data from this query batcher into copyTo and
     * copies the results into newEntityContext if they are not already there.
     * @param newEntityContext
     * @param copyTo
     */
    public void copyTo(EntityContext newEntityContext, QueryBatcher copyTo) {
        if (copyTo != this) {
            copyTo.queries.clear();
            copyTo.results.clear();
            copyTo.queries.addAll( queries );
        }
        if (copyTo.results != results) {
            for (QueryResult<?> result : results) {
                copyTo.results.add( result.copyResultTo(newEntityContext) );
            }
        }
    }

    public boolean isEmpty() {
      return queries.isEmpty();
    }

}
