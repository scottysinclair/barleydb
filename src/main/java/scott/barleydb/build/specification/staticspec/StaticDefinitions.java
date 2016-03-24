package scott.barleydb.build.specification.staticspec;

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

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.JoinTypeSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.PrimaryKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.UniqueConstraintSpec;

public abstract class StaticDefinitions {

    protected final String namespace;

    private Map<String,StaticDefinitions> dependentStaticDefinitions = new HashMap<String, StaticDefinitions>();

    public StaticDefinitions(String namespace) {
        this.namespace = namespace;
    }

    public Collection<StaticDefinitions> getDependentStaticDefinitions() {
        return Collections.unmodifiableCollection( dependentStaticDefinitions.values() );
    }

    public String getNamespace() {
        return namespace;
    }

    /*
     * Provides the default compiler / vm ordering
     */
    public Class<?>[] getOrder() {
        return getClass().getClasses();
    }

    /**
     * Override for custom post processing
     * @param definitionsSpec
     */
    public void postProcess(DefinitionsSpec definitionsSpec) {
        //System.out.println("Post Processing: " + definitionsSpec.getNamespace());
        addPrimaryKeyConstraints(definitionsSpec);
        addForeignKeyConstraints(definitionsSpec);
        setUniqueConstraintNames(definitionsSpec);
    }

    /**
     * Gets the StaticDefinitions instance which is responsible for
     * defining the given class.
     * @param innerClass
     * @return
     */
    public StaticDefinitions resolveStaticDefinitionFor(Class<?> innerClass) {
        if (innerClass.getEnclosingClass() == getClass()) {
            return this;
        }
        for (StaticDefinitions def: dependentStaticDefinitions.values()) {
            def = def.resolveStaticDefinitionFor(innerClass);
            if (def != null) {
                return def;
            }
        }
        throw new IllegalStateException("cannot resolve static definitions instance for " + innerClass);
    }

    public boolean isAstract(Class<?> entityDefinition) {
        return entityDefinition.getAnnotation(AbstractEntity.class) != null;
    }

    /**
     * Creates a fully qualified class name for an entity model
     * @param entityDefinition the class defining the entity.
     * @return
     */
    public abstract String createFullyQualifiedModelClassName(Class<?> entityDefinition);

    public abstract String createFullyQualifiedQueryClassName(Class<?> entityDefinition);

    public abstract String createFullyQualifiedEnumClassName(Class<?> enumDefinition);

    /**
     * Creates a DB column name for the given NodeSpec.
     * @param nodeSpec
     * @return
     */
    public String createColumnName(NodeSpec nodeSpec) {
        RelationSpec relation = nodeSpec.getRelationSpec();
        if (relation == null) {
            if (nodeSpec.getName() == null) {
                throw new IllegalStateException("Cannot make column name, no relation and node name not set.");
            }
            return convertJavaNameToColumnName(nodeSpec.getName());
        }
        if (relation != null && relation.isForeignKeyRelation()) {
            if (relation.getEntitySpec() != null) {
                return createForeignKeyColumnNameForEntitySpec(relation.getEntitySpec());
            }
        }
        return null;
    }

    /**
     * Creates a db column name which is a foreign key reference to the given entity.
     * @param entitySpec
     * @return
     */
    protected String createForeignKeyColumnNameForEntitySpec(EntitySpec entitySpec) {
        return entitySpec.getTableName() + "_id";
    }

    /**
     * Gets the table name for the entity, going to the
     * parent entity in the case of inheritance.
     */
    public String getTableName(Class<?> entityDefinition) {
        String tableName = getDeclaredTableName(entityDefinition);
        if (tableName != null) {
            return tableName;
        }
        if (extendsEntity(entityDefinition)) {
            return getInheritedTableName(entityDefinition);
        }
        return null;
    }

    /**
     * Converts a Java camel-case name to a DB name with underscores.
     * @param javaName
     * @return
     */
    private String convertJavaNameToColumnName(String javaName) {
        Matcher m = Pattern.compile("([A-Z])").matcher(javaName);
        StringBuilder sb = new StringBuilder();
        int i=0;
        while(m.find()) {
            sb.append(javaName.substring(i, m.start()));
            sb.append('_');
            sb.append(m.group().toLowerCase());
            i = m.end();
        }
        sb.append(javaName.substring(i));
        return sb.toString();
    }

    /**
     *
     * Checks to see if the entity definition indicates that
     * it extends another entity definition.
     *
     * @param entityDefinition
     * @return true iff it extends another entity.
     */
    public boolean extendsEntity(Class<?> entityDefinition) {
        return entityDefinition.getAnnotation(ExtendsEntity.class) != null;
    }

    /**
     * Gets the table name declared on the given entity definition.
     * @param entityDefinition
     * @return
     */
    private String getDeclaredTableName(Class<?> entityDefinition) {
        Entity e = entityDefinition.getAnnotation(Entity.class);
        if (e != null) {
            return e.value();
        }
        AbstractEntity ae = entityDefinition.getAnnotation(AbstractEntity.class);
        if (ae != null) {
            return ae.value();
        }
        return null;
    }

    /**
     * Gets the table name for the entity based on inheritance
     * between the entities.
     * @param entityDefinition
     * @return
     */
    private String getInheritedTableName(Class<?> entityDefinition) {
        Class<?> superClass =  entityDefinition.getSuperclass();
        if (superClass.equals(Object.class)) {
            return null;
        }
        return getTableName(superClass);
    }

    protected final void setAllNamesToUpperCase(DefinitionsSpec definitionsSpec) {
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            entitySpec.setTableName( entitySpec.getTableName().toUpperCase() );
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (nodeSpec.getColumnName() != null) {
                    nodeSpec.setColumnName( nodeSpec.getColumnName().toUpperCase() );
                }
            }
            PrimaryKeyConstraintSpec pkConstraintSpec = entitySpec.getPrimaryKeyConstraint();
            if (pkConstraintSpec != null) {
                pkConstraintSpec.setName( pkConstraintSpec.getName().toUpperCase() );
            }
            for (ForeignKeyConstraintSpec spec: entitySpec.getForeignKeyConstraints()) {
                spec.setName( spec.getName().toUpperCase() );
            }
            for (UniqueConstraintSpec spec: entitySpec.getUniqueConstraints()) {
                spec.setName( spec.getName().toUpperCase() );
            }
        }
    }

    private void addPrimaryKeyConstraints(DefinitionsSpec definitionsSpec) {
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            //System.out.println("Checking primary key constraint to " + entitySpec.getTableName());
            Collection<NodeSpec> key = entitySpec.getPrimaryKeyNodes(false);
            if (key != null) {
                //System.out.println("Added primary key constraint to " + entitySpec.getTableName());
                createPrimaryKeyConstraint(entitySpec, key);
            }
        }
    }

    private void addForeignKeyConstraints(DefinitionsSpec definitionsSpec) {
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                RelationSpec relationSpec = nodeSpec.getRelationSpec();
                if (relationSpec != null) {
                    if (relationSpec.isForeignKeyRelation() && foreignConstraintDesired(nodeSpec, relationSpec)) {
                        createForeignKeyConstraint(entitySpec, nodeSpec, relationSpec);
                    }
                }
            }
        }
    }

    private void setUniqueConstraintNames(DefinitionsSpec definitionsSpec) {
        for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
            for (UniqueConstraintSpec spec: entitySpec.getUniqueConstraints()) {
                if (spec.getName() == null) {
                    spec.setName( createUniqueConstraintName(entitySpec, spec) );
                }
            }
        }
    }

    private void createPrimaryKeyConstraint(EntitySpec entitySpec, Collection<NodeSpec> key) {
        String keySpecName = createPrimaryKeyConstraintName( entitySpec, key );
        PrimaryKeyConstraintSpec pkSpec = new PrimaryKeyConstraintSpec(keySpecName, key);
        entitySpec.setPrimaryKeyConstraint( pkSpec );
    }

    /**
     * Override for custom behavior
     *
     * By default all foreign key relations require a constraint so this
     * method returns true.
     *
     * @param nodeSpec the FK node on the entity
     * @param relationSpec the realtion information
     * @return
     */
    protected boolean foreignConstraintDesired(NodeSpec nodeSpec, RelationSpec relationSpec) {
        return true;
    }

    private void createForeignKeyConstraint(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        String keySpecName = createForeignKeyConstraintName( entitySpec, nodeSpec, relationSpec );
        EntitySpec toEntitySpec = relationSpec.getEntitySpec();

        Collection<NodeSpec> toPrimaryKey = toEntitySpec.getPrimaryKeyNodes(true);
        if (toPrimaryKey == null) {
            throw new IllegalStateException("Cannot create foreign key reference to entity " + toEntitySpec.getClassName() + " which  has no primary key");
        }
        ForeignKeyConstraintSpec spec = new ForeignKeyConstraintSpec(keySpecName, asList(nodeSpec), toEntitySpec, toPrimaryKey);
        entitySpec.add(spec);
    }


    /**
     * Creates s FK constraint name fk_<from_table>_<to_table>
     * @param entitySpec
     * @param nodeSpec
     * @param relationSpec
     * @return
     */
    protected String createForeignKeyConstraintName(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        return "fk_" + entitySpec.getTableName() + "_" + relationSpec.getEntitySpec().getTableName();
    }

    protected String createPrimaryKeyConstraintName(EntitySpec entitySpec, Collection<NodeSpec> key) {
        return "pk_" + entitySpec.getTableName();
    }

    /**
     * uc_<table name>_<index>
     * @param entitySpec
     * @param spec
     * @return
     */
    protected String createUniqueConstraintName(EntitySpec entitySpec, UniqueConstraintSpec spec) {
        int i = entitySpec.indexOf(spec);
        if (i == -1) {
            throw new IllegalStateException(spec + " does not belong to " + entitySpec.getClassName());
        }
        return "uc_" + entitySpec.getTableName() + "_" + (i+1);
    }

    protected void add(StaticDefinitions spec) {
        dependentStaticDefinitions.put(spec.getNamespace(), spec);
    }

    public abstract JoinTypeSpec getJoinType(EntitySpec entitySpec, RelationSpec relationSpec);

}
