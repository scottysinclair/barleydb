package scott.sort.server.jdbc.query;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.query.PreparingQueryStatementException;
import scott.sort.api.exception.execution.query.QueryConnectionRequiredException;
import scott.sort.server.jdbc.helper.PreparedStatementCache;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;
import scott.sort.server.jdbc.resources.ConnectionResources;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


class PreparedStatementQueryCache extends PreparedStatementCache<PreparingQueryStatementException, QueryConnectionRequiredException> {

    public PreparedStatementQueryCache(PreparedStatementHelper<PreparingQueryStatementException> helper) {
        super(helper);
    }

    @Override
    protected ConnectionResources getConnectionResources(EntityContext entityContext) throws QueryConnectionRequiredException {
        return ConnectionResources.getMandatoryForQuery(entityContext);
    }

}
