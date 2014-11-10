package scott.sort.build.specification.staticspec.processor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;
import scott.sort.api.specification.SpecRegistry;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;
import scott.sort.build.specification.staticspec.AbstractEntity;
import scott.sort.build.specification.staticspec.Entity;
import scott.sort.build.specification.staticspec.ExtendsEntity;
import scott.sort.build.specification.staticspec.StaticDefinitions;

public class StaticDefinitionProcessor {
	
	public DefinitionsSpec process(StaticDefinitions staticDefs, SpecRegistry registry) {
		return new StateFullWorker(registry).buildDefinitionsSpecAndDependencies(staticDefs);
	}
	
	/**
	 * Performs the work and contains state relevent to it.
	 * @author scott
	 *
	 */
	private class StateFullWorker {
		
		/**
		 * The registry of definitions
		 * We will add any definitions we create to it.
		 */
		private SpecRegistry registry;
		
		/**
		 * A map of lookup objects to EntitySpecs.
		 * The relations initially refer to EntitySpecs via the lookup objects.
		 */
		private final Map<Object, EntitySpec> entitySpecByStaticKey = new HashMap<>();
		
		/**
		 * The entity definitions which are in the progress of being created.
		 */
		private final Set<Class<?>> inprogress = new HashSet<>();
		
		public StateFullWorker(SpecRegistry registry) {
			this.registry = registry;
		}

		/**
		 * Builds the DefinitionsSpec from the static definition including all dependencies.
		 * @param staticDefs
		 * @return
		 */
		public DefinitionsSpec buildDefinitionsSpecAndDependencies(StaticDefinitions staticDefs) {
			for (StaticDefinitions dependent: staticDefs.getDependentStaticDefinitions()) {
				buildDefinitionsSpecAndDependencies(dependent);
			}
			DefinitionsSpec spec =  buildDefinitionsSpec(staticDefs);
			return spec;
		}

		/**
		 * Builds the DefinitionsSpec for the static definition.
		 * @param staticDefs
		 * @return
		 */
		private DefinitionsSpec buildDefinitionsSpec(StaticDefinitions staticDefs) {
			//System.out.println("processing definition " + staticDefs.getClass().getName());
			DefinitionsSpec definitionsSpec = new DefinitionsSpec();
			definitionsSpec.setNamespace( staticDefs.getNamespace() );
			
			for (StaticDefinitions dependency: staticDefs.getDependentStaticDefinitions()) {
				definitionsSpec.addImport( dependency.getNamespace() );
			}
			
			for (Class<?> innerClass: staticDefs.getOrder()) {
				if (definesEntity( innerClass )) {
					definitionsSpec.add( buildEntitySpec(staticDefs, innerClass ) );
				}
			}
			resolveRelations(staticDefs, definitionsSpec);
			postProcess(staticDefs, definitionsSpec);
			definitionsSpec.verify();
			registry.add(definitionsSpec);
			return definitionsSpec;
		}

		/**
		 * Performs any post processing tasks once the DefinitionsSpec has been fully built
		 * and all EntitySpecs in relations resolved.
		 * @param staticDefs
		 * @param spec
		 */
		private void postProcess(StaticDefinitions staticDefs, DefinitionsSpec spec) {
			staticDefs.postProcess(spec);
		}
		
		/**
		 * processes the relations and replaces entity id lookups with the actual EntitySpecs
		 * Also sets the relation FK column name if it is still null. 
		 */
		private void resolveRelations(StaticDefinitions staticDefs, DefinitionsSpec spec) {
			for (EntitySpec entitySpec: spec.getEntitySpecs()) {
				for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
					RelationSpec relationSpec = nodeSpec.getRelationSpec();
					if (relationSpec != null && relationSpec.getEntitySpec() == null) {
						EntitySpec es = entitySpecByStaticKey.get( relationSpec.getEntitySpecIdentifier() );
						if (es == null) {
							throw new IllegalStateException("Could not resolve realtion id " + relationSpec.getEntitySpecIdentifier());
						}
						relationSpec.setEntitySpec(es);
						relationSpec.setEntitySpecIdentifier(null);
						if (nodeSpec.getColumnName() == null) {
							/*
							 * We try and set any column names 
							 * which are still null.
							 * 
							 * The relation info which we now have
							 * is usually relevent for the column name
							 * 
							 */
							nodeSpec.setColumnName( staticDefs.createColumnName(nodeSpec) );
						}
					}
				}
			}
		}

		/**
		 * Processes an entity definition returning a full EntitySpec
		 * @param staticDefs
		 * @param entityDefinitionClass
		 * @return
		 */
		private EntitySpec buildEntitySpec(StaticDefinitions staticDefs, Class<?> entityDefinitionClass) {
			if (!inprogress.add( entityDefinitionClass )) {
				return null;
			}
			//System.out.println("building EntitySpec " + entityDefinitionClass.getName());

			try {
				EntitySpec spec = new EntitySpec();
			
				/*
				 * Set any inheritance relationship if it exists.
				 */
				if (staticDefs.extendsEntity(entityDefinitionClass)) {
					EntitySpec superSpec = getSuperEntitySpec(spec, staticDefs, entityDefinitionClass);
					spec.setParentEntitySpec(superSpec);
				}
			
				spec.setClassName( staticDefs.createFullyQualifiedClassName(entityDefinitionClass) );
				spec.setTableName( staticDefs.getTableName(entityDefinitionClass) );
				/*
				 * Already store the entitySpec to key relation
				 */
				entitySpecByStaticKey.put(entityDefinitionClass, spec);
				/*
				 * Add NodeSpecs defined on any interfaces.
				 */
				addInterfaceNodeSpecs(staticDefs, spec, entityDefinitionClass);
				/*
				 * Adds NodeSpecs defined on the entity definition.
				 */
				addOwnNodeSpecs(staticDefs, spec, entityDefinitionClass);
				/*
				 * Adds constraints defined on any interfaces.
				 */
				addInterfaceConstraints(staticDefs, spec, entityDefinitionClass);
				/*
				 * Adds constraints defined on the entity definition.
				 */
				addOwnConstraints(staticDefs, spec, entityDefinitionClass, false);
				return spec;
			}
			finally {
				inprogress.remove(entityDefinitionClass);
			}
		}

		/**
		 * Processes the NodeSpecs on the interface definition, adding copies of them to the given EntitySpecs.
		 * @param staticDefs
		 * @param entitySpec
		 * @param interfaceDefinitionClass
		 */
		private void addInterfaceNodeSpecs(StaticDefinitions staticDefs, EntitySpec entitySpec, Class<?> interfaceDefinitionClass) {
			if (!interfaceDefinitionClass.getEnclosingClass().isAssignableFrom(staticDefs.getClass()))  {
				throw new IllegalStateException(interfaceDefinitionClass + " not defined in " + staticDefs.getClass());
			}
			for (Class<?> interfaceClass: interfaceDefinitionClass.getInterfaces()) {
				addInterfaceNodeSpecs(staticDefs.resolveStaticDefinitionFor(interfaceClass), entitySpec, interfaceClass);
			}
			if (interfaceDefinitionClass.isInterface()) {
				for (FieldValuePair<NodeSpec> fieldValue: getStaticFieldValues(interfaceDefinitionClass, NodeSpec.class)) {
					/*
					 * We clone the specs from the interfaces so that each entity has it's own copy
					 */
					NodeSpec interfaceNodeSpec = fieldValue.value;
					/*
					 * Set the interface nodespec name if it is null
					 * Required for copying of interface constraints based on NodeSpec name 
					 */
					if (interfaceNodeSpec.getName() == null) {
						interfaceNodeSpec.setName( fieldValue.field.getName() );
					}
					NodeSpec clonedSpec = interfaceNodeSpec.clone();
					processNodeSpecAndAddToEntity(staticDefs, entitySpec, clonedSpec, fieldValue.field);
				}
			}
		}
		
		/**
		 * Processes the NodeSpec fields on the entity definition, adding them to the EntitySpec.
		 * @param staticDefs
		 * @param entitySpec
		 * @param entityDefinitionClass
		 */
		private void addOwnNodeSpecs(StaticDefinitions staticDefs, EntitySpec entitySpec, Class<?> entityDefinitionClass) {
			if (!entityDefinitionClass.getEnclosingClass().isAssignableFrom(staticDefs.getClass()))  {
				throw new IllegalStateException(entityDefinitionClass + " not defined in " + staticDefs.getClass());
			}
			for (FieldValuePair<NodeSpec> fieldValue: getStaticFieldValues(entityDefinitionClass, NodeSpec.class)) {
				processNodeSpecAndAddToEntity(staticDefs, entitySpec, fieldValue.value, fieldValue.field);
			}
		}
		
		/**
		 * Processes any interface constraints adding clones of them
		 * to the EntitySpec.
		 *  
		 * @param staticDefs
		 * @param entitySpec
		 * @param entityDefinitionClass
		 */
		private void addInterfaceConstraints(StaticDefinitions staticDefs, EntitySpec entitySpec, Class<?> entityDefinitionClass) {
			if (!entityDefinitionClass.getEnclosingClass().isAssignableFrom(staticDefs.getClass()))  {
				throw new IllegalStateException(entityDefinitionClass + " not defined in " + staticDefs.getClass());
			}
			for (Class<?> interfaceClass: entityDefinitionClass.getInterfaces()) {
				addInterfaceConstraints(staticDefs.resolveStaticDefinitionFor(interfaceClass), entitySpec, interfaceClass);
			}
			if (entityDefinitionClass.isInterface()) {
				for (FieldValuePair<UniqueConstraintSpec> fieldValue: getStaticFieldValues(entityDefinitionClass, UniqueConstraintSpec.class)) {
					/*
					 * We get a copy of the unique constraint spec for our own EntitySpec
					 */
					UniqueConstraintSpec clonedSpec = fieldValue.value.newCopyFor(entitySpec);
					entitySpec.add(clonedSpec);
				}
			}
		}
		
		/**
		 * Processes unique constraints which are part of the static entity definition
		 * adding them to the EntitySpec.
		 * @param staticDefs
		 * @param entitySpec
		 * @param entityDefinitionClass
		 * @param b
		 */
		private void addOwnConstraints(StaticDefinitions staticDefs, EntitySpec entitySpec, Class<?> entityDefinitionClass, boolean b) {
			for (FieldValuePair<UniqueConstraintSpec> fieldValue: getStaticFieldValues(entityDefinitionClass, UniqueConstraintSpec.class)) {
				entitySpec.add(fieldValue.value);
			}
		}
		
	
		/**
		 * Gets the EntitySpec of the super type of the given EntitySpec.
		 * @param spec the 
		 * @param staticDefs
		 * @param entityDefinitionClass
		 * @return the parent EntitySpec
		 */
		private EntitySpec getSuperEntitySpec(EntitySpec spec, StaticDefinitions staticDefs, Class<?> entityDefinitionClass) {
			Class<?> superClass = entityDefinitionClass.getSuperclass();
			EntitySpec superSpec = entitySpecByStaticKey.get(superClass);
			if (superSpec == null) {
				if (entityDefinitionClass.getEnclosingClass() == superClass.getEnclosingClass()) {
					superSpec = buildEntitySpec(staticDefs, superClass);
				}
				else {
					throw new IllegalStateException("Could not resolve super entity spec " + superClass);
				}
			}
			return superSpec;
		}
	}

	private void processNodeSpecAndAddToEntity(StaticDefinitions staticDefs, EntitySpec entitySpec, NodeSpec nodeSpec, Field field) {
		if (nodeSpec.getName() == null) {
			nodeSpec.setName( field.getName() );
		}
		if (nodeSpec.getColumnName() == null) {
			/*
			 * If the node is a FK relation, then we try and calculate the name 
			 * after the relation is resolved, not now..
			 */
			if (nodeSpec.getRelationSpec() == null) {
				nodeSpec.setColumnName( staticDefs.createColumnName( nodeSpec ) );
			}
		}
		entitySpec.add(nodeSpec);
		nodeSpec.setEntity(entitySpec);
	}
	
	/**
	 * Checks if the given class defines an entity.
	 * @param type
	 * @return
	 */
	private boolean definesEntity(Class<?> type) {
		if (type.getAnnotation(Entity.class) != null) {
			return true;
		}
		if (type.getAnnotation(AbstractEntity.class) != null) {
			return true;
		}
		if (type.getAnnotation(ExtendsEntity.class) != null) {
			return true;
		}
		return false;
	}	
	
	/**
	 * Gets the field value pairs of all fields of a given type for a Class.
	 * @param entityDefinitionClass
	 * @param type
	 * @return
	 */
	private <T> Collection<FieldValuePair<T>> getStaticFieldValues(Class<?> entityDefinitionClass, Class<T> type) {
		Collection<FieldValuePair<T>> result = new LinkedList<>();
		for (Field field: entityDefinitionClass.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (!type.isAssignableFrom( field.getType() )) {
				continue;
			}
			try {
				@SuppressWarnings("unchecked")
				T value = (T)field.get(null);
				result.add( new FieldValuePair<>(field, value) );
			} 
			catch (IllegalArgumentException | IllegalAccessException x) {
				throw new IllegalStateException("Could not call field " + field, x);
			}
		}
		return result;
	}
	

	/**
	 * A Java reflection Field and it's associated value.
	 * @author scott
	 *
	 * @param <T>
	 */
	private class FieldValuePair<T> {
		public final Field field;
		public final T value;
		public FieldValuePair(Field field, T value) {
			this.field = field;
			this.value = value;
		}
	}
}
