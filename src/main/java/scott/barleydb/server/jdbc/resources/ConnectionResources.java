package scott.barleydb.server.jdbc.resources;

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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.persist.PersistTransactionRequiredException;
import scott.barleydb.api.exception.execution.query.QueryConnectionRequiredException;
import scott.barleydb.server.jdbc.vendor.Database;

public class ConnectionResources {

    public static ConnectionResources get(EntityContext entityContext) {
        return (ConnectionResources)entityContext.getResource(ConnectionResources.class.getName(), false);
    }

    public static ConnectionResources getMandatoryForPersist(EntityContext entityContext) throws PersistTransactionRequiredException {
        ConnectionResources cr = (ConnectionResources)entityContext.getResource(ConnectionResources.class.getName(), false);
        if (cr == null) {
            throw new PersistTransactionRequiredException("No transaction for entity context");
        }
        return cr;
    }

    public static ConnectionResources getMandatoryForQuery(EntityContext entityContext) throws QueryConnectionRequiredException {
        ConnectionResources cr = (ConnectionResources)entityContext.getResource(ConnectionResources.class.getName(), false);
        if (cr == null) {
            throw new QueryConnectionRequiredException("No transaction for entity context");
        }
        return cr;
    }

    public static ConnectionResources set(EntityContext entityContext, Connection connection, Database database) {
        ConnectionResources cr = new ConnectionResources(entityContext, connection, database);
        entityContext.setResource(ConnectionResources.class.getName(), cr);
        return cr;
    }

    private final Set<EntityContext> entityContexts = new HashSet<>();

    private final Connection connection;

    private final Database database;

    public ConnectionResources(EntityContext entityContext, Connection connection, Database database) {
        this.entityContexts.add(entityContext);
        this.connection = connection;
        this.database = database;
    }

    public Collection<EntityContext> getEntityContexts() {
        return entityContexts;
    }

    public Connection getConnection() {
        return connection;
    }

    public Database getDatabase() {
        return database;
    }

    public void close() throws SQLException {
        removeFromEntityContexts();
        getConnection().close();
    }

    public void removeFromEntityContexts() {
        for (EntityContext ec: entityContexts) {
            ec.removeResource(ConnectionResources.class.getName());
        }
    }

}
