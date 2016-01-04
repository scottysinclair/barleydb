package scott.sort.api.specification;

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

import scott.sort.api.config.RelationType;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
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

    public static <E extends Enum<E>> NodeSpec mandatoryEnum(Class<E> type, JdbcType jdbcType) {
        return enumValue(null, type, jdbcType, Nullable.NOT_NULL);
    }

    public static <E extends Enum<E>> NodeSpec enumValue(String name, Class<E> type, JdbcType jdbcType, Nullable nullable) {
        NodeSpec spec = new NodeSpec();
        spec.setName( name );
        spec.setJavaType(JavaType.ENUM);
        spec.setJdbcType(jdbcType);
        spec.setEnumType(type);
        spec.setNullable( nullable );
        return spec;
    }

    public static <E extends Enum<E>> NodeSpec mandatoryFixedEnum(E value, JdbcType jdbcType) {
        @SuppressWarnings("unchecked")
        NodeSpec spec = mandatoryEnum(value.getClass(), jdbcType);
        spec.setFixedValue(value);
        return spec;
    }

    public static NodeSpec optionallyRefersTo(Class<?> type) {
        return relation(
                RelationType.REFERS,
                type,
                null,
                Nullable.NULL);
    }

    public static NodeSpec optionallyRefersTo(Class<?> type, String columnName) {
        return relation(
                RelationType.REFERS,
                type,
                columnName,
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
        spec.setRelationSpec(new RelationSpec(relationType, type, null, null));
        spec.setColumnName(columnName);
        spec.setNullable( nullable );
        return spec;
    }

    public static NodeSpec ownsMany(Class<?> type, NodeSpec nodeType) {
        return manyRelation(RelationType.OWNS, type, nodeType, null);
    }

    public static NodeSpec ownsMany(Class<?> type, NodeSpec nodeType, NodeSpec onwardJoinNodeType) {
        return manyRelation(RelationType.OWNS, type, nodeType, onwardJoinNodeType);
    }

    public static NodeSpec refersToMany(Class<?> type, NodeSpec nodeType) {
        return manyRelation(RelationType.REFERS, type, nodeType, null);
    }
    
    public static NodeSpec sortedBy(NodeSpec byMe, NodeSpec nodeSpec) {
        nodeSpec.getRelationSpec().setSortNode(byMe);
        return nodeSpec;
    }

    public static NodeSpec manyRelation(RelationType relationType, Class<?> type, NodeSpec nodeType, NodeSpec onwardJoinNodeType) {
        NodeSpec spec = new NodeSpec();
        spec.setRelationSpec(new RelationSpec(relationType, type, nodeType, onwardJoinNodeType));
        return spec;
    }

    public static UniqueConstraintSpec uniqueConstraint(NodeSpec... nodeSpec) {
        return new UniqueConstraintSpec(nodeSpec);
    }

}
