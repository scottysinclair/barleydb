package scott.sort.server.jdbc.persister;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.persist.PersistTransactionRequiredException;
import scott.sort.api.exception.persist.PreparingPersistStatementException;
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

    public PreparedStatementPersistCache(Definitions definitions) {
        super(new PersistPreparedStatementHelper(definitions));
    }

    @Override
    protected ConnectionResources getConnectionResources(EntityContext entityContext) throws PersistTransactionRequiredException {
        return ConnectionResources.getMandatoryForPersist(entityContext);
    }



}
