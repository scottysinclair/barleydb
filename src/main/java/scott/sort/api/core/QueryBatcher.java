package scott.sort.api.core;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.query.QueryResult;

public class QueryBatcher implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<QueryObject<?>> queries = new LinkedList<>();
    private List<QueryResult<?>> results = new LinkedList<>();

    public void addQuery(QueryObject<?>... queryObjects) {
        for (QueryObject<?> qo : queryObjects) {
            queries.add(qo);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> QueryResult<T> getResult(int index, Class<T> type) {
        return (QueryResult<T>) results.get(index);
    }

    public int size() {
        return queries.size();
    }

    public Iterable<QueryObject<?>> getQueries() {
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

}
