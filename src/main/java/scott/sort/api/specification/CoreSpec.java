package scott.sort.api.specification;

import scott.sort.api.config.RelationType;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.Nullable;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;

/**
 * Provides scott.sort.definitions data type specifications required by the framework.
 * @author scott
 *
 */
public class CoreSpec {

    public static String lowerFirstChar(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static <E extends Enum<E>> NodeSpec mandatoryEnum(Class<E> type) {
        return enumValue(null, type, Nullable.NOT_NULL);
    }

    public static <E extends Enum<E>> NodeSpec enumValue(String name, Class<E> type, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setName( name );
        spec.setJavaType(JavaType.ENUM);
        spec.setEnumType(type);
        spec.setNullable( nullable );
        return spec;
    }

    public static <E extends Enum<E>> NodeSpec mandatoryFixedEnum(E value) {
        @SuppressWarnings("unchecked")
        NodeSpec spec = mandatoryEnum(value.getClass());
        spec.setFixedValue(value);
        return spec;
    }

    public static NodeSpec optionalRefersTo(Class<?> type) {
        return relation(
                RelationType.REFERS,
                type,
                null,
                Nullable.NULL);
    }

    public static NodeSpec mandatoryRefersTo(Class<?> type) {
        return relation(
                RelationType.REFERS,
                type,
                null,
                Nullable.NOT_NULL);
    }

    public static NodeSpec mandatoryRefersTo(Class<?> type, String columnName) {
        return relation(
                RelationType.REFERS,
                type,
                columnName,
                Nullable.NOT_NULL);
    }

    public static NodeSpec optionallyOwns(Class<?> type) {
        return relation(
                RelationType.OWNS,
                type,
                null,
                Nullable.NULL);
    }

    public static NodeSpec optionallyOwns(Class<?> type, String columnName) {
        return relation(
                RelationType.OWNS,
                type,
                columnName,
                Nullable.NULL);
    }

    public static NodeSpec dependsOn(Class<?> type) {
        return relation(
                RelationType.DEPENDS,
                type,
                null,
                Nullable.NOT_NULL);
    }

    public static NodeSpec dependsOn(Class<?> type, String columnName) {
        return relation(
                RelationType.DEPENDS,
                type,
                columnName,
                Nullable.NOT_NULL);
    }

    public static NodeSpec relation(RelationType relationType, Class<?> type, String columnName, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setRelationSpec(new RelationSpec(relationType, type, null));
        spec.setColumnName(columnName);
        spec.setNullable( nullable );
        return spec;
    }

    public static NodeSpec ownsMany(Class<?> type, NodeSpec nodeType) {
        return manyRelation(RelationType.OWNS, type, nodeType);
    }

    public static NodeSpec manyRelation(RelationType relationType, Class<?> type, NodeSpec nodeType) {
        NodeSpec spec = new NodeSpec();
        spec.setRelationSpec(new RelationSpec(relationType, type, nodeType));
        return spec;
    }

    public static UniqueConstraintSpec uniqueConstraint(NodeSpec... nodeSpec) {
        return new UniqueConstraintSpec(nodeSpec);
    }

}