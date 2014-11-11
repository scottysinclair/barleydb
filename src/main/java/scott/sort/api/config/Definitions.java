package scott.sort.api.config;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import scott.sort.api.core.QueryRegistry;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.query.QueryObject;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;

public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String namespace;

    private List<String> references = new LinkedList<>();

    private Map<String, EntityType> entities = new HashMap<String, EntityType>();

    private ProxyFactory proxyFactory;

    private DefinitionsSet definitionsSet;

    /**
     * contains the standard query (no joins, no conditions) for each entity type
     */
    private final QueryRegistry internalQueryRegistry = new QueryRegistry();

    public static Definitions create(DefinitionsSpec definitionsSpec) {
        Definitions definitions = new Definitions( definitionsSpec.getNamespace() );
        for (String importedSpec: definitionsSpec.getImports()) {
            definitions.references.add( importedSpec );
        }
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            definitions.entities.put(entitySpec.getClassName(), EntityType.create(definitions, entitySpec));
        }
        return definitions;
    }

    private Definitions(String namespace) {
        this.namespace = namespace;
    }

    public void registerProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * Gets all of the queries in the standard internal query registry.
     * @return
     */
    public Collection<QueryObject<?>> getAllQueries() {
        return internalQueryRegistry.getAll();
    }

    public List<ProxyFactory> getProxyFactories() {
        List<ProxyFactory> facs = new LinkedList<ProxyFactory>();
        if (proxyFactory != null) {
            facs.add(proxyFactory);
        }
        for (String namespace : references) {
            facs.addAll(definitionsSet.getDefinitions(namespace).getProxyFactories());
        }
        return facs;
    }

    public void registerQueries(QueryObject<?>... qos) {
        internalQueryRegistry.register(qos);
    }

    /**
     * Called when added to a definitions set
     * @param definitionsSet
     */
    public void setDefinitionsSet(DefinitionsSet definitionsSet) {
        this.definitionsSet = definitionsSet;
    }

    public String getNamespace() {
        return namespace;
    }

    public EntityType getEntityTypeForClass(Class<?> type, boolean mustExist) {
        return getEntityTypeMatchingInterface(type.getName(), mustExist);
    }

    public EntityType getEntityTypeMatchingInterface(String interfaceName, boolean mustExist) {
        EntityType et = entities.get(interfaceName);
        if (et != null) {
            return et;
        }
        if (definitionsSet != null) {
            for (String namespace : references) {
                Definitions d = definitionsSet.getDefinitions(namespace);
                EntityType e = d.getEntityTypeMatchingInterface(interfaceName, false);
                if (e != null) {
                    return e;
                }
            }
        }
        if (mustExist) {
            throw new IllegalStateException("EntityType '" + interfaceName + "' must exist");
        }
        return null;
    }

    public List<EntityType> getEntityTypesExtending(EntityType parentEntityType) {
        List<EntityType> childTypes = new LinkedList<EntityType>();
        for (EntityType et : entities.values()) {
            if (et.isDirectChildOf(parentEntityType)) {
                childTypes.add(et);
            }
        }

        if (definitionsSet != null) {
            for (String namespace : references) {
                Definitions d = definitionsSet.getDefinitions(namespace);
                childTypes.addAll(d.getEntityTypesExtending(parentEntityType));
            }
        }
        return childTypes;
    }

    public QueryRegistry newUserQueryRegistry() {
        QueryRegistry copy = internalQueryRegistry.clone();
        if (definitionsSet != null) {
            for (String namespace : references) {
                Definitions d = definitionsSet.getDefinitions(namespace);
                copy.addAll(d.newUserQueryRegistry());
            }
        }
        return copy;
    }

    public QueryObject<Object> getQuery(EntityType entityType) {
        return this.<Object> getQuery(entityType.getInterfaceName());
    }

    public <T> QueryObject<T> getQuery(Class<T> clazz) {
        return this.<T> getQuery(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public <T> QueryObject<T> getQuery(String interfaceName) {
        QueryObject<Object> qo = internalQueryRegistry.getQuery(interfaceName);
        if (qo != null) {
            return (QueryObject<T>) qo;
        }
        if (definitionsSet != null) {
            for (String namespace : references) {
                Definitions d = definitionsSet.getDefinitions(namespace);
                qo = d.getQuery(interfaceName);
                if (qo != null) {
                    return (QueryObject<T>) qo;
                }
            }
        }
        throw new IllegalStateException("Query for entity type '" + interfaceName + "' does not exist.");
    }

}
