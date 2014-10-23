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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.*;

import scott.sort.api.core.QueryRegistry;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.query.QueryObject;

@XmlRootElement(name = "definitions")
@XmlAccessorType(XmlAccessType.FIELD)
public class Definitions implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute(name = "namespace")
    private String namespace;

    @XmlElement(name = "namespace")
    private List<String> references = new LinkedList<>();

    @XmlElement(name = "entity")
    private Map<String, EntityType> entities = new HashMap<String, EntityType>();

    private ClassLoader proxyClassLoader;

    @XmlTransient
    private ProxyFactory proxyFactory;

    private DefinitionsSet definitionsSet;

    /**
     * contains the standard query (no joins, no conditions) for each entity type
     */
    private final QueryRegistry internalQueryRegistry = new QueryRegistry();

    public Definitions() {
        proxyClassLoader = Thread.currentThread().getContextClassLoader();
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

    public ClassLoader getProxyClassLoader() {
        return proxyClassLoader;
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

    public static Definitions start(String namespace) {
        Definitions d = new Definitions();
        d.namespace = namespace;
        return d;
    }

    public Definitions references(String namespace) {
        references.add(namespace);
        return this;
    }

    public EntityTypeDefinition newEntity(Class<?> clazz, String tableName) {
        return newEntity(clazz.getName(), tableName);
    }

    public EntityTypeDefinition newEntity(String interfaceName, String tableName) {
        EntityTypeDefinition etd = new EntityTypeDefinition();
        return etd.interfaceName(interfaceName).tableName(tableName);
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

    public class EntityTypeDefinition {
        private EntityType et;
        private String interfaceName;
        private String tableName;
        private String keyNodeName;
        private boolean abstractEntity;
        private Class<?> parentEntity;
        private List<NodeDefinition> nodeDefinitions = new LinkedList<NodeDefinition>();

        public EntityTypeDefinition interfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public EntityTypeDefinition tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public void abstractEntity(boolean abstractEntity) {
            this.abstractEntity = abstractEntity;
        }

        public void parentEntity(Class<?> parentEntity) {
            this.parentEntity = parentEntity;
        }

        public EntityTypeDefinition withKey(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
            this.keyNodeName = name;
            return withValue(name, javaType, columnName, jdbcType);
        }

        public EntityTypeDefinition withValue(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .value(name, javaType, columnName, jdbcType)
                            .end());
            return this;
        }

        public EntityTypeDefinition withOptimisticLock(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .value(name, javaType, columnName, jdbcType)
                            .optimisticLock()
                            .end());
            return this;
        }

        public EntityTypeDefinition withEnum(String name, Class<? extends Enum<?>> enumType, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .enumm(name, enumType, columnName, jdbcType)
                            .end());
            return this;
        }

        public EntityTypeDefinition withFixedValue(String name, JavaType javaType, String columnName, JdbcType jdbcType, Object value) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .value(name, javaType, columnName, jdbcType)
                            .fixedValue(value)
                            .end());
            return this;
        }

        @SuppressWarnings("unchecked")
        public EntityTypeDefinition withFixedEnum(String name, Enum<?> value, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .enumm(name, (Class<? extends Enum<?>>) value.getClass(), columnName, jdbcType)
                            .fixedValue(value)
                            .end());
            return this;
        }

        public EntityTypeDefinition withMany(String name, Class<?> clazz, String foreignNodeName) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .many(name, clazz.getName(), foreignNodeName, null)
                            .end());
            return this;
        }

        public EntityTypeDefinition dependsOnMany(String name, String interfaceName, String foreignNodeName, String joinProperty) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .many(name, interfaceName, foreignNodeName, joinProperty)
                            .dependsOn()
                            .end());
            return this;
        }

        public EntityTypeDefinition ownsMany(String name, Class<?> clazz, String foreignNodeName) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .many(name, clazz.getName(), foreignNodeName, null)
                            .owns()
                            .end());
            return this;
        }

        public EntityTypeDefinition ownsMany(String name, Class<?> clazz, String foreignNodeName, String toManyProperty) {
            return ownsMany(name, clazz.getName(), foreignNodeName, toManyProperty);
        }

        public EntityTypeDefinition ownsMany(String name, String interfaceName, String foreignNodeName, String toManyProperty) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .many(name, interfaceName, foreignNodeName, toManyProperty)
                            .owns()
                            .end());
            return this;
        }

        public EntityTypeDefinition withOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .ref(name, clazz.getName(), columnName, jdbcType)
                            .end());
            return this;
        }

        public EntityTypeDefinition dependsOnOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .ref(name, clazz.getName(), columnName, jdbcType)
                            .dependsOn()
                            .end());
            return this;
        }

        public EntityTypeDefinition ownsOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                            .ref(name, clazz.getName(), columnName, jdbcType)
                            .owns()
                            .end());
            return this;
        }

        public EntityTypeDefinition newEntity(String interfaceName, String tableName) {
            complete();
            return Definitions.this.newEntity(interfaceName, tableName);
        }

        public EntityTypeDefinition newEntity(Class<?> clazz, String tableName) {
            complete();
            return Definitions.this.newEntity(clazz, tableName);
        }

        public EntityTypeDefinition newAbstractEntity(Class<?> clazz, String tableName) {
            complete();
            EntityTypeDefinition et = Definitions.this.newEntity(clazz, tableName);
            et.abstractEntity(true);
            return et;
        }

        public EntityTypeDefinition newChildEntity(Class<?> clazz, Class<?> parentEntity) {
            complete();
            EntityTypeDefinition et = Definitions.this.newEntity(clazz, tableName);
            et.parentEntity(parentEntity);
            return et;
        }

        public Definitions complete() {
            if (et == null) {
                String parentTypeName = null;
                if (parentEntity != null) {
                    EntityType parentEt = entities.get(parentEntity.getName());
                    if (parentEt == null) {
                        throw new IllegalStateException("Could not find parent entity type '" + parentEntity.getName() + "'");
                    }
                    //make sure the parent nodes are added to the start of the list
                    List<NodeDefinition> parentNodes = new ArrayList<NodeDefinition>(parentEt.getNodeDefinitions());
                    //we reverse the list since we add any missing parent node to pos 0
                    Collections.reverse(parentNodes);
                    for (NodeDefinition nd : parentNodes) {
                        if (missingNodeDefinition(nd.getName(), nodeDefinitions)) {
                            nodeDefinitions.add(0, nd.clone());
                        }
                    }
                    tableName = parentEt.getTableName();
                    keyNodeName = parentEt.getKeyNodeName();
                    parentTypeName = parentEntity.getName();
                }
                this.et = new EntityType(Definitions.this, interfaceName, abstractEntity, parentTypeName, tableName, keyNodeName, nodeDefinitions);
                entities.put(et.getInterfaceName(), et);
            }
            return Definitions.this;
        }

        private boolean missingNodeDefinition(String name, List<NodeDefinition> nds) {
            for (NodeDefinition nd : nds) {
                if (nd.getName().equals(name)) {
                    return false;
                }
            }
            return true;
        }
    }
}
