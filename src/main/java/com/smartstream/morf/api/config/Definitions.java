package com.smartstream.morf.api.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.*;

import com.smartstream.morf.api.core.QueryRegistry;
import com.smartstream.morf.api.core.proxy.ProxyFactory;
import com.smartstream.morf.api.core.types.JavaType;
import com.smartstream.morf.api.core.types.JdbcType;
import com.smartstream.morf.api.query.QueryObject;

@XmlRootElement(name = "definitions")
@XmlAccessorType(XmlAccessType.FIELD)
public class Definitions implements Serializable {

	private static final long serialVersionUID = 1L;

	@XmlAttribute(name="namespace")
	private String namespace;

	@XmlElement(name="namespace")
	private List<String> references = new LinkedList<>();

	@XmlElement(name="entity")
	private List<EntityType> entities = new LinkedList<EntityType>();

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

	public List<ProxyFactory> getProxyFactories() {
	    List<ProxyFactory> facs = new LinkedList<ProxyFactory>();
	    if (proxyFactory != null) {
	        facs.add( proxyFactory );
	    }
	    for (String namespace: references) {
	        facs.addAll( definitionsSet.getDefinitions(namespace).getProxyFactories() );
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

	public List<EntityType> getEntities() {
		return entities;
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
		references.add( namespace );
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
		for (EntityType et: entities) {
			if (et.getInterfaceName().equals(interfaceName)) {
				return et;
			}
		}
		if (definitionsSet != null) {
			for (String namespace: references) {
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

	public QueryRegistry newUserQueryRegistry() {
		QueryRegistry copy = internalQueryRegistry.clone();
		if (definitionsSet != null) {
			for (String namespace: references) {
				Definitions d = definitionsSet.getDefinitions(namespace);
				copy.addAll(d.newUserQueryRegistry());
			}
		}
		return copy;
	}

	public QueryObject<Object> getQuery(EntityType entityType) {
		return this.<Object>getQuery(entityType.getInterfaceName());
	}
	public <T> QueryObject<T> getQuery(Class<T> clazz) {
		return this.<T>getQuery(clazz.getName());
	}

	@SuppressWarnings("unchecked")
    public <T> QueryObject<T> getQuery(String interfaceName) {
		QueryObject<Object> qo = internalQueryRegistry.getQuery(interfaceName);
		if (qo != null) {
			return (QueryObject<T>)qo;
		}
		if (definitionsSet != null) {
			for (String namespace: references) {
				Definitions d = definitionsSet.getDefinitions(namespace);
				qo = d.getQuery(interfaceName);
				if (qo != null) {
					return (QueryObject<T>)qo;
				}
			}
		}
		throw new IllegalStateException("Query for entity type '" + interfaceName + "' does not exist.");
	}


	public class EntityTypeDefinition {
		private EntityType et;
		private String interfaceName;
		private String tableName;
		private String keyColumn;
		private List<NodeDefinition> nodeDefinitions = new LinkedList<NodeDefinition>();
		public EntityTypeDefinition interfaceName(String interfaceName) {
			this.interfaceName = interfaceName;
			return this;
		}
		public EntityTypeDefinition tableName(String tableName) {
			this.tableName = tableName;
			return this;
		}
		public EntityTypeDefinition withKey(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
			this.keyColumn = name;
			return withValue(name, javaType, columnName, jdbcType);
		}
		public EntityTypeDefinition withValue(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.value(name, javaType, columnName, jdbcType)
						.end() );
			return this;
		}
		public EntityTypeDefinition withOptimisticLock(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.value(name, javaType, columnName, jdbcType)
						.optimisticLock()
						.end() );
			return this;
		}
		public EntityTypeDefinition withEnum(String name, Class<? extends Enum<?>> enumType, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.enumm(name, enumType, columnName, jdbcType)
						.end() );
			return this;
		}
		public EntityTypeDefinition withFixedValue(String name, JavaType javaType, String columnName, JdbcType jdbcType, Object value) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.value(name, javaType, columnName, jdbcType)
						.fixedValue(value)
						.end() );
			return this;
		}
		@SuppressWarnings("unchecked")
        public EntityTypeDefinition withFixedEnum(String name, Enum<?> value, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.enumm(name, (Class<? extends Enum<?>>)value.getClass(), columnName, jdbcType)
						.fixedValue(value)
						.end() );
			return this;
		}
		public EntityTypeDefinition withMany(String name, Class<?> clazz, String foreignNodeName) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.many(name, clazz.getName(), foreignNodeName, null)
						.end() );
			return this;
		}
        public EntityTypeDefinition dependsOnMany(String name, String interfaceName, String foreignNodeName, String joinProperty) {
            nodeDefinitions.add(
                    new NodeDefinition.Builder()
                        .many(name, interfaceName, foreignNodeName, joinProperty)
                        .dependsOn()
                        .end() );
            return this;
        }
		public EntityTypeDefinition ownsMany(String name, Class<?> clazz, String foreignNodeName) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.many(name, clazz.getName(), foreignNodeName, null)
						.owns()
						.end() );
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
                        .end() );
            return this;
        }
		public EntityTypeDefinition withOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.ref(name, clazz.getName(), columnName, jdbcType)
						.end() );
			return this;
		}
		public EntityTypeDefinition dependsOnOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.ref(name, clazz.getName(), columnName, jdbcType)
						.dependsOn()
						.end() );
			return this;
		}
		public EntityTypeDefinition ownsOne(String name, Class<?> clazz, String columnName, JdbcType jdbcType) {
			nodeDefinitions.add(
					new NodeDefinition.Builder()
						.ref(name, clazz.getName(), columnName, jdbcType)
						.owns()
						.end() );
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
		public Definitions complete() {
			if (et == null) {
				this.et = new EntityType(Definitions.this, interfaceName, tableName, keyColumn, nodeDefinitions);
				entities.add(et);
			}
			return Definitions.this;
		}
	}
}
