package scott.sort.server.jdbc.resources;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.persist.PersistTransactionRequiredException;
import scott.sort.api.exception.query.QueryConnectionRequiredException;
import scott.sort.server.jdbc.database.Database;

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

    public void removeFromEntityContexts() {
        for (EntityContext ec: entityContexts) {
            ec.removeResource(ConnectionResources.class.getName());
        }
    }



}
