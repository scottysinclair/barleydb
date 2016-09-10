package scott.barleydb.server.jdbc.persist;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.persist.PersistTransactionRequiredException;
import scott.barleydb.api.exception.execution.persist.PreparingPersistStatementException;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.helper.PreparedStatementCache;
import scott.barleydb.server.jdbc.resources.ConnectionResources;

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


class PreparedStatementPersistCache extends PreparedStatementCache<PreparingPersistStatementException, PersistTransactionRequiredException> {

    public PreparedStatementPersistCache(RuntimeProperties runtimeProps, JdbcEntityContextServices jdbcEntityContextServices, Definitions definitions) {
        super(new PersistPreparedStatementHelper(runtimeProps, jdbcEntityContextServices, definitions));
    }

    @Override
    protected ConnectionResources getConnectionResources(EntityContext entityContext) throws PersistTransactionRequiredException {
        return ConnectionResources.getMandatoryForPersist(entityContext);
    }



}
