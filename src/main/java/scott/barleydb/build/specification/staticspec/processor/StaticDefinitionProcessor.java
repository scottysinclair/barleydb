package scott.barleydb.build.specification.staticspec.processor;

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.api.specification.SuppressionSpec;
import scott.barleydb.api.specification.constraint.UniqueConstraintSpec;
import scott.barleydb.build.specification.staticspec.AbstractEntity;
import scott.barleydb.build.specification.staticspec.Entity;
import scott.barleydb.build.specification.staticspec.Enumeration;
import scott.barleydb.build.specification.staticspec.ExtendsEntity;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;
import scott.barleydb.build.specification.staticspec.SuppressFromGeneratedCode;
import scott.barleydb.build.specification.staticspec.SuppressSetter;

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
         * A map of lookup objects to EnumSpecs.
         * The relations initially refer to EnumSpecs via the lookup objects.
         */
        private final Map<Object, EnumSpec> enumSpecByStaticKey = new HashMap<>();

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
                if (definesEnum( innerClass )) {
                    definitionsSpec.add( buildEnumSpec(staticDefs, innerClass ) );
                }
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
                        relationSpec.setJoinType( staticDefs.getJoinType(entitySpec, relationSpec));
                        /*
                         * If the FK jdbc type is null then take the
                         * JDBC type of the FK from the entity primary key.
                         *
                         */
                        if (relationSpec.isForeignKeyRelation()) {
                            Collection<NodeSpec> key = es.getPrimaryKeyNodes(true);
                            if (key.size() != 1) {
                                throw new IllegalStateException("Invalid key " + key + " for entity: " + es);
                            }
                            if (nodeSpec.getJdbcType() == null) {
                                nodeSpec.setJdbcType( key.iterator().next().getJdbcType() );
                                nodeSpec.setLength( key.iterator().next().getLength() );
                            }

                        }

                        if (nodeSpec.getColumnName() == null) {
                            /*
                             * We try and set any column names
                             * which are still null.
                             *
                             * The relation info which we now have
                             * is usually relevant for the column name
                             *
                             */
                            nodeSpec.setColumnName( staticDefs.createColumnName(nodeSpec) );
                        }
                    }
                }
            }
        }

        private EnumSpec buildEnumSpec(StaticDefinitions staticDefs, Class<?> enumDefinitionClass) {
            EnumSpec enumSpec = new EnumSpec();
            enumSpec.setClassName( staticDefs.createFullyQualifiedEnumClassName( enumDefinitionClass ));
            List<EnumValueSpec> enumValues = new LinkedList<>();
            for (Field f: enumDefinitionClass.getFields()) {
                EnumValueSpec enumValue;
                try {
                    enumValue = new EnumValueSpec((int)f.get(null), f.getName());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException("Could not create enum value for field " + f.getName());
                }
                enumValues.add( enumValue );
            }
            enumSpec.setEnumValues(enumValues);
            /*
             * refer to enumSpecs by their defining class, for later lookup.
             */
            enumSpecByStaticKey.put(enumDefinitionClass, enumSpec);
            return enumSpec;
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


                spec.setAbstractEntity( staticDefs.isAstract( entityDefinitionClass ) );
                spec.setClassName( staticDefs.createFullyQualifiedModelClassName(entityDefinitionClass) );
                spec.setQueryClassName( staticDefs.createFullyQualifiedQueryClassName(entityDefinitionClass) );
                spec.setDtoClassName( staticDefs.createFullyQualifiedDtoClassName(entityDefinitionClass) );
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

        private void processNodeSpecAndAddToEntity(StaticDefinitions staticDefs, EntitySpec entitySpec, NodeSpec nodeSpec, Field field) {
            if (nodeSpec.getName() == null) {
                nodeSpec.setName( field.getName() );
            }
            if (field.getAnnotation(SuppressFromGeneratedCode.class) != null) {
                nodeSpec.setSuppression(SuppressionSpec.GENERATED_CODE);
            }
            if (field.getAnnotation(SuppressSetter.class) != null) {
                nodeSpec.setSuppression(SuppressionSpec.GENERATED_CODE_SETTER);
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
            if (nodeSpec.getEnumSpecIdentifier() != null && nodeSpec.getEnumSpec() == null) {
                EnumSpec enumSpec = enumSpecByStaticKey.get( nodeSpec.getEnumSpecIdentifier() );
                if (enumSpec == null) {
                    throw new IllegalStateException("Could not resolve enum id " + nodeSpec.getEnumSpecIdentifier());
                }
                nodeSpec.setEnumSpec( enumSpec );
            }
            entitySpec.add(nodeSpec);
            nodeSpec.setEntity(entitySpec);
        }

    }

    private boolean definesEnum(Class<?> type) {
        return type.getAnnotation(Enumeration.class) != null;
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
