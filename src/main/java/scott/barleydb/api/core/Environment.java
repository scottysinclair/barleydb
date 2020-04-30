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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.DefinitionsSet;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.model.ProxyCreationException;
import scott.barleydb.api.persist.AccessRightsChecker;
import scott.barleydb.api.persist.Auditor;
import scott.barleydb.api.persist.LoggingAuditor;
import scott.barleydb.api.persist.NoopAccessRightsChecker;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.server.jdbc.query.QueryPreProcessor;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.IEntityContextServices;

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

    private RuntimeProperties defaultRuntimeProperties;

    private final IEntityContextServices entityContextServices;

    private AccessRightsChecker accessRightsChecker = new NoopAccessRightsChecker();

    private Auditor auditor = new LoggingAuditor();

    public Environment(IEntityContextServices entityContextServices) {
        this.entityContextServices = entityContextServices;
        this.definitionsSet = new DefinitionsSet();
    }

    @PostConstruct
    public void loadDefinitions() {
        DefinitionsSet ds = this.entityContextServices.getDefinitionsSet();
        if (ds != null) {
            definitionsSet.addAll(ds);
        }
    }


    public void preProcess(QueryObject<?> query, Definitions definitions) {
        if (queryPreProcessor != null) {
            queryPreProcessor.preProcess(query, definitions);
        }
    }

    public DefinitionsSet getDefinitionsSet() {
      return definitionsSet;
    }

    public RuntimeProperties getDefaultRuntimeProperties() {
        return defaultRuntimeProperties;
    }

    public void setDefaultRuntimeProperties(RuntimeProperties defaultRuntimeProperties) {
        this.defaultRuntimeProperties = defaultRuntimeProperties;
    }

    public AccessRightsChecker getAccessRightsChecker() {
      return accessRightsChecker;
    }

    public void setAccessRightsChecker(AccessRightsChecker accessRightsChecker) {
      this.accessRightsChecker = accessRightsChecker;
    }

    public Auditor getAuditor() {
      return auditor;
    }

    public void setAuditor(Auditor auditor) {
      this.auditor = auditor;
    }

    public IEntityContextServices getEntityContextServices() {
        return entityContextServices;
    }

    public void setQueryPreProcessor(QueryPreProcessor queryPreProcessor) {
        this.queryPreProcessor = queryPreProcessor;
    }

    public void setAutocommit(EntityContext entityContext, boolean value) throws SortServiceProviderException {
        entityContextServices.setAutoCommit(entityContext, value);
    }

    public void joinTransaction(EntityContext newContext, EntityContext context) {
        entityContextServices.joinTransaction(newContext, context);
    }

    public boolean getAutocommit(EntityContext entityContext) throws SortServiceProviderException {
        return entityContextServices.getAutoCommit(entityContext);
    }

    public void rollback(EntityContext entityContext) throws SortServiceProviderException {
        entityContextServices.rollback(entityContext);
    }

    public void commit(EntityContext entityContext) throws SortServiceProviderException {
        entityContextServices.commit(entityContext);
    }

    public void close(EntityContext entityContext) throws SortServiceProviderException {
        entityContextServices.close(entityContext);
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
        if (defaultRuntimeProperties == null) {
            throw new IllegalStateException("Default runtime properties are not set");
        }
        definitionsSet.addDefinitions(definitions);
    }

    public IEntityContextServices services() {
        return entityContextServices;
    }

    /**
     * Null safe overriding of runtime properties
     * @param props
     * @return
     */
    public RuntimeProperties overrideProps(RuntimeProperties props) {
        if (props == null) {
            if (defaultRuntimeProperties == null) {
                throw new IllegalStateException("No default runtime properties for environment.");
            }
            return defaultRuntimeProperties;
        }
        else if (defaultRuntimeProperties == null) {
            return props;
        }
        else {
            return props.override( defaultRuntimeProperties );
        }
    }


    public <T> T generateProxy(Entity entity) throws ProxyCreationException {
        for (ProxyFactory fac : entity.getEntityContext().getDefinitions().getProxyFactories()) {
            if (fac != null) {
                T proxy = fac.newProxy(entity);
                if (proxy != null) {
                    return proxy;
                }
            }
        }
        throw new ProxyCreationException("No proxy factory registered for namespace " + entity.getEntityContext().getNamespace());
    }

}
