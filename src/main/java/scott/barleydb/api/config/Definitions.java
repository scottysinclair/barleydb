package scott.barleydb.api.config;

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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import scott.barleydb.api.core.QueryRegistry;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;

public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String namespace;

    private List<String> references = new LinkedList<>();

    /**
     * entities by class FQN (perhaps the class need not exist??)
     */
    private Map<String, EntityType> entities = new HashMap<String, EntityType>();

    private ProxyFactory proxyFactory;

    private DefinitionsSet definitionsSet;

    /**
     * contains the standard query (no joins, no conditions) for each entity
     * type
     */
    private final QueryRegistry internalQueryRegistry = new QueryRegistry();

    public static Definitions create(DefinitionsSpec definitionsSpec) {
        Definitions definitions = new Definitions(definitionsSpec.getNamespace());
        for (String importedSpec : definitionsSpec.getImports()) {
            definitions.references.add(importedSpec);
        }
        for (EntitySpec entitySpec : definitionsSpec.getEntitySpecs()) {
            definitions.entities.put(entitySpec.getClassName(), EntityType.create(definitions, entitySpec));
        }
        return definitions;
    }

    public List<String> getReferences() {
        return Collections.unmodifiableList(references);
    }

    private Definitions(String namespace) {
        this.namespace = namespace;
    }

    public void registerProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
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
     *
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

    /**
     * gets the entity type which matches an interface FQN
     *
     * @param interfaceName
     * @param mustExist
     * @return
     */
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

    public Collection<EntityType> getEntityTypes() {
        return Collections.unmodifiableCollection(entities.values());
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
        return getQuery(entityType, true);
    }

    public QueryObject<Object> getQuery(EntityType entityType, boolean mustExist) {
        return this.<Object>getQuery(entityType.getInterfaceName(), mustExist);
    }

    public <T> QueryObject<T> getQuery(Class<T> clazz) {
        return getQuery(clazz, true);
    }

    public <T> QueryObject<T> getQuery(Class<T> clazz, boolean mustExist) {
        return this.<T>getQuery(clazz.getName(), mustExist);
    }

    @SuppressWarnings("unchecked")
    public <T> QueryObject<T> getQuery(String interfaceName, boolean mustExist) {
        QueryObject<Object> qo = internalQueryRegistry.getQuery(interfaceName);
        if (qo != null) {
            return (QueryObject<T>) qo;
        }
        if (definitionsSet != null) {
            for (String namespace : references) {
                Definitions d = definitionsSet.getDefinitions(namespace);
                qo = d.getQuery(interfaceName, mustExist);
                if (qo != null) {
                    return (QueryObject<T>) qo;
                }
            }
        }
        if (mustExist) {
            throw new IllegalStateException("Query for entity type '" + interfaceName + "' does not exist.");
        }
        return null;
    }

}
