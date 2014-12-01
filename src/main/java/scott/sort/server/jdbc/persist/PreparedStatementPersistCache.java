package scott.sort.server.jdbc.persist;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.persist.PersistTransactionRequiredException;
import scott.sort.api.exception.execution.persist.PreparingPersistStatementException;
import scott.sort.server.jdbc.JdbcEntityContextServices;
import scott.sort.server.jdbc.helper.PreparedStatementCache;
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


class PreparedStatementPersistCache extends PreparedStatementCache<PreparingPersistStatementException, PersistTransactionRequiredException> {

    public PreparedStatementPersistCache(JdbcEntityContextServices jdbcEntityContextServices, Definitions definitions) {
        super(new PersistPreparedStatementHelper(jdbcEntityContextServices, definitions));
    }

    @Override
    protected ConnectionResources getConnectionResources(EntityContext entityContext) throws PersistTransactionRequiredException {
        return ConnectionResources.getMandatoryForPersist(entityContext);
    }



}
