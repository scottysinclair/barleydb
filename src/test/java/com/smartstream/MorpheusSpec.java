package com.smartstream;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.core.types.Nullable;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;
import scott.sort.build.specification.staticspec.StaticDefinitions;

/**
 * Provides common datatype specifications used by morpheus
 *
 * @author scott
 *
 */
public class MorpheusSpec extends StaticDefinitions {

    protected class Relation {
        private final Class<?> from;
        private final Class<?> to;
        public Relation(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public boolean matches(EntitySpec from, EntitySpec to) {
            return MorpheusSpec.this.matches(this.from, from) && MorpheusSpec.this.matches(this.to, to);
        }
    }

    private final List<Relation> excludedForeignKeyConstraints = new LinkedList<>();

    public MorpheusSpec(String namespace) {
        super(namespace);
    }

    @Override
    public void postProcess(DefinitionsSpec definitionsSpec) {
        super.postProcess(definitionsSpec);
        setAllNamesToUpperCase(definitionsSpec);
    }

    private boolean matches(Class<?> entityDefinition, EntitySpec entitySpec) {
        String fqn = createFullyQualifiedClassName(entityDefinition);
        return entitySpec.getClassName().equals(fqn);
    }

    /**
     * The entity package to be the same as the definitions namespace + ".model"
     * @param entityDefinition
     * @return
     */
    public String createFullyQualifiedClassName(Class<?> entityDefinition) {
        return namespace + ".model." + entityDefinition.getSimpleName();
    }

    public static NodeSpec longPrimaryKey() {
        NodeSpec spec = mandatoryLongValue();
        spec.setColumnName("ID");
        spec.setPrimaryKey(true);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryLongValue() {
        return mandatoryLongValue(null);
    }

    public static NodeSpec mandatoryLongValue(String columnName) {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName(columnName);
        spec.setJavaType(JavaType.LONG);
        spec.setJdbcType(JdbcType.BIGINT);
        return spec;
    }

    public static NodeSpec mandatoryBooleanValue() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.BOOLEAN);
        spec.setJdbcType(JdbcType.INT);
        return spec;
    }

    public static NodeSpec mandatoryIntegerValue() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.INTEGER);
        spec.setJdbcType(JdbcType.INT);
        return spec;
    }

    public static NodeSpec mandatoryVarchar50() {
        return varchar(50, Nullable.NOT_NULL);
    }

    public static NodeSpec varchar(int length, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.STRING);
        spec.setJdbcType(JdbcType.VARCHAR);
        spec.setLength(length);
        spec.setNullable(nullable);
        return spec;
    }

    public static NodeSpec name() {
        return mandatoryVarchar50();
    }

    public static NodeSpec optimisticLock() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.LONG);
        spec.setJdbcType(JdbcType.TIMESTAMP);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec uuid() {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName("UUID");
        spec.setJavaType(JavaType.UUID);
        spec.setJdbcType(JdbcType.TIMESTAMP);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    @Override
    protected boolean foreignConstraintDesired(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        for (Relation relation: excludedForeignKeyConstraints) {
            if (relation.matches(entitySpec, relationSpec.getEntitySpec())) {
                return false;
            }
        }
        return super.foreignConstraintDesired(entitySpec, nodeSpec, relationSpec);
    }

    protected void excludeForeignKeyConstraint(Relation realtion) {
        excludedForeignKeyConstraints.add(realtion);
    }

    @Override
    protected String createForeignKeyColumnNameForEntitySpec(EntitySpec entitySpec) {
        //parent class does table name + "id"
        String fkColumnName = super.createForeignKeyColumnNameForEntitySpec(entitySpec);
        //we remove the module name which we know is there for a morpheus table
        return removePrefix(fkColumnName);
    }

    /**
     * For morpheus the constrain does not have the table's module prefix.
     * So we have PK_USER instead of PK_MAC_USER
     */
    @Override
    protected String createPrimaryKeyConstraintName(EntitySpec entitySpec, Collection<NodeSpec> key) {
        return "pk_" + removePrefix(entitySpec.getTableName());
    }

    /**
     * Creates s FK constraint name fk_<from_table>_<to_table>
     *
     * The module prefix is removed
     *
     * @param entitySpec
     * @param nodeSpec
     * @param relationSpec
     * @return
     */
    @Override
    protected String createForeignKeyConstraintName(EntitySpec entitySpec, NodeSpec nodeSpec, RelationSpec relationSpec) {
        return "fk_" + removePrefix(entitySpec.getTableName()) + "_" + removePrefix(relationSpec.getEntitySpec().getTableName());
    }


    @Override
    protected String createUniqueConstraintName(EntitySpec entitySpec, UniqueConstraintSpec spec) {
        int i = entitySpec.indexOf(spec);
        if (i == -1) {
            throw new IllegalStateException(spec + " does not belong to " + entitySpec.getClassName());
        }
        return "uc_" + removePrefix(entitySpec.getTableName()) + "_" + (i+1);
    }

    private String removePrefix(String value) {
        int i = value.indexOf('_');
        return value.substring(i+1);
    }

}
