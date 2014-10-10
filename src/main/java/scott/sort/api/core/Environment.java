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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.DefinitionsSet;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.proxy.EntityProxy;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.persister.SequenceGenerator;
import scott.sort.server.jdbc.queryexecution.QueryPreProcessor;

/**
 * Exists on both the client and the server
 *
 * @author scott
 *
 */
public final class Environment {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(Environment.class);

    private final DefinitionsSet definitionsSet;

    private QueryPreProcessor queryPreProcessor;

    private final IEntityContextServices entityContextServices;

    private SequenceGenerator sequenceGenerator;

    public void preProcess(QueryObject<?> query, Definitions definitions) {
        if (queryPreProcessor != null) {
            queryPreProcessor.preProcess(query, definitions);
        }
    }

    public IEntityContextServices getEntityContextServices() {
        return entityContextServices;
    }

    public void setQueryPreProcessor(QueryPreProcessor queryPreProcessor) {
        this.queryPreProcessor = queryPreProcessor;
    }

    public Environment(IEntityContextServices entityContextServices) {
        this.entityContextServices = entityContextServices;
        this.definitionsSet = new DefinitionsSet();
    }

    public SequenceGenerator getSequenceGenerator() {
        return sequenceGenerator;
    }

    public void setSequenceGenerator(SequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    public void setAutocommit(EntityContext entityContext, boolean value) throws SortJdbcException {
        entityContextServices.setAutocommit(entityContext, value);
    }

    public void joinTransaction(EntityContext newContext, EntityContext context) {
        entityContextServices.joinTransaction(newContext, context);
    }

    public boolean getAutocommit(EntityContext entityContext) throws SortJdbcException {
        return entityContextServices.getAutocommit(entityContext);
    }

    public void rollback(EntityContext entityContext) throws SortJdbcException {
        entityContextServices.rollback(entityContext);
    }

    /**
     *
     * @param namespace
     * @return
     * @throws IllegalStateException if not found
     */
    public Definitions getDefinitions(String namespace) {
        return definitionsSet.getDefinitions(namespace);
    }

    public void addDefinitions(Definitions definitions) {
        definitionsSet.addDefinitions(definitions);
    }

    public IEntityContextServices services() {
        return entityContextServices;
    }

    public <T> T generateProxy(Entity entity) throws ClassNotFoundException {
        //LOG.debug("generateProxy for " + entity);
        for (ProxyFactory fac : entity.getEntityContext().getDefinitions().getProxyFactories()) {
            if (fac != null) {
                T proxy = fac.newProxy(entity);
                if (proxy != null) {
                    return proxy;
                }
            }
        }
        return EntityProxy.generateProxy(entity.getEntityContext().getDefinitions().getProxyClassLoader(), entity);
    }

}
