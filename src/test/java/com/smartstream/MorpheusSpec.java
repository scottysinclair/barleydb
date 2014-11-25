package com.smartstream;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.core.types.Nullable;
import scott.sort.api.specification.CoreSpec;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.JoinTypeSpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;
import scott.sort.api.specification.SuppressionSpec;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;
import scott.sort.build.specification.staticspec.StaticDefinitions;

/**
 * Provides common datatype specifications used by morpheus
 *
 * @author scott
 *
 */
public class MorpheusSpec extends StaticDefinitions {

    private final List<StaticRelation> excludedForeignKeyConstraints = new LinkedList<>();

    private final Map<StaticRelation,String> renamedForeignKeyConstraints = new HashMap<>();

    public MorpheusSpec(String namespace) {
        super(namespace);
    }

    @Override
    public void postProcess(DefinitionsSpec definitionsSpec) {
        super.postProcess(definitionsSpec);
        setAllNamesToUpperCase(definitionsSpec);
    }

    /**
     * The entity package to be the same as the definitions namespace + ".model"
     * @param entityDefinition
     * @return
     */
    public String createFullyQualifiedModelClassName(Class<?> entityDefinition) {
        return namespace + ".model." + entityDefinition.getSimpleName();
    }

    @Override
    public String createFullyQualifiedQueryClassName(Class<?> entityDefinition) {
        return namespace + ".query.Q" + entityDefinition.getSimpleName();
    }

    @Override
    public JoinTypeSpec getJoinType(EntitySpec entitySpec, RelationSpec relationSpec) {
        return JoinTypeSpec.LEFT_OUTER_JOIN;
    }

    public static NodeSpec longPrimaryKey() {
        NodeSpec spec = mandatoryLongValue();
        spec.setColumnName("ID");
        spec.setPrimaryKey(true);
        spec.setNullable(Nullable.NOT_NULL);
        spec.setSuppression(SuppressionSpec.GENERATED_CODE_SETTER);
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
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryBooleanValue() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.BOOLEAN);
        spec.setJdbcType(JdbcType.INT);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryIntegerValue() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.INTEGER);
        spec.setJdbcType(JdbcType.INT);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static NodeSpec mandatoryVarchar50() {
        return varchar(null, 50, Nullable.NOT_NULL);
    }

    public static NodeSpec optionalVarchar50() {
        return varchar(null, 50, Nullable.NULL);
    }

    public static NodeSpec mandatoryVarchar150() {
        return varchar(null, 150, Nullable.NULL);
    }

    public static NodeSpec mandatoryVarchar50(String columnName) {
        return varchar(columnName, 50, Nullable.NOT_NULL);
    }

    public static NodeSpec mandatoryNonStreamingLob() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.BYTE_ARRAY);
        spec.setJdbcType(JdbcType.BLOB);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    public static <E extends Enum<E>> NodeSpec mandatoryEnum(Class<E> type) {
        return CoreSpec.mandatoryEnum(type, JdbcType.INT);
    }

    public static <E extends Enum<E>> NodeSpec mandatoryFixedEnum(E value) {
        return CoreSpec.mandatoryFixedEnum(value, JdbcType.INT);
    }

    public static NodeSpec varchar(int length, Nullable nullable) {
        return varchar(null, length, nullable);
    }

    public static NodeSpec varchar(String columnName, int length, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.STRING);
        spec.setColumnName( columnName );
        spec.setJdbcType(JdbcType.VARCHAR);
        spec.setLength(length);
        spec.setNullable(nullable);
        return spec;
    }

    public static NodeSpec name() {
        return mandatoryVarchar50();
    }

    public static NodeSpec name(String columnName) {
        return mandatoryVarchar50(columnName);
    }

    public static NodeSpec optimisticLock() {
        NodeSpec spec = new NodeSpec();
        spec.setJavaType(JavaType.LONG);
        spec.setJdbcType(JdbcType.TIMESTAMP);
        spec.setNullable(Nullable.NOT_NULL);
        spec.setOptimisticLock(true);
        return spec;
    }

    public static NodeSpec uuid() {
        NodeSpec spec = new NodeSpec();
        spec.setColumnName("UUID");
        spec.setJavaType(JavaType.STRING);
        spec.setJdbcType(JdbcType.CHAR);
        spec.setLength(60);
        spec.setNullable(Nullable.NOT_NULL);
        return spec;
    }

    @Override
    protected boolean foreignConstraintDesired(NodeSpec nodeSpec, RelationSpec relationSpec) {
        for (StaticRelation relation: excludedForeignKeyConstraints) {
            if (relation.matches(nodeSpec, relationSpec.getEntitySpec())) {
                return false;
            }
        }
        return super.foreignConstraintDesired(nodeSpec, relationSpec);
    }

    protected void excludeForeignKeyConstraint(StaticRelation realtion) {
        excludedForeignKeyConstraints.add(realtion);
    }

    protected void renameForeignKeyConstraint(StaticRelation relation, String newName) {
        renamedForeignKeyConstraints.put(relation, newName);
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
        /*
         * Check if there is an explicit renaming.
         */
        for (StaticRelation staticRel: renamedForeignKeyConstraints.keySet()) {
            if (staticRel.matches(nodeSpec, relationSpec.getEntitySpec())) {
                return renamedForeignKeyConstraints.get(staticRel);
            }
        }
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

    protected class StaticRelation {
        private final NodeSpec fromNode;
        public StaticRelation(NodeSpec fromNode) {
            this.fromNode = fromNode;
            if (this.fromNode.getRelationSpec() == null) {
                throw new IllegalStateException("Node has no relation: " + fromNode);
            }
        }

        /**
         * The NodeSpec that we hold must have the proper EntitySpec set on it for the
         * matching to work.
         * @param from
         * @param to
         * @return
         */
        public boolean matches(NodeSpec from, EntitySpec to) {
            if (!Objects.equals(this.fromNode, from)) {
                return false;
            }
            if (!Objects.equals(this.fromNode.getRelationSpec().getEntitySpec(), to)) {
                return false;
            }
            return true;
        }
    }

}