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

import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.query.QueryResult;

public class QueryBatcher {

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

    public QueryBatcher copyResultTo(EntityContext newEntityContext) {
        if (results.isEmpty() || results.get(0).getEntityContext() == newEntityContext) {
            return this;
        }
        QueryBatcher newBatchResult = new QueryBatcher();
        for (QueryResult<?> result : results) {
            QueryResult<?> newResult = result.copyResultTo(newEntityContext);
            newBatchResult.addResult(newResult);
        }
        return newBatchResult;
    }

}
