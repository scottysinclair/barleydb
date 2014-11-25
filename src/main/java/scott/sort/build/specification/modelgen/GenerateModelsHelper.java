package scott.sort.build.specification.modelgen;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.IOException;
import java.io.Writer;

import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;

public class GenerateModelsHelper {

    protected void writeJavaType(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        if (relationSpec != null) {
            NodeSpec ownwardJoin = nodeSpec.getRelationSpec().getOwnwardJoin();
            if (relationSpec.isForeignKeyRelation()) {
                if (ownwardJoin != null) {
                    out.write(getModelSimpleClassName(ownwardJoin.getEntity()));
                }
                else {
                    out.write(getModelSimpleClassName(relationSpec.getEntitySpec()));
                }
            }
            else {
                out.write("List<");
                if (ownwardJoin != null) {
                    out.write(getModelSimpleClassName(ownwardJoin.getRelationSpec().getEntitySpec()));
                }
                else {
                    out.write(getModelSimpleClassName(relationSpec.getEntitySpec()));
                }
                out.write(">");
            }
        }
        else if (nodeSpec.getEnumType() != null) {
            out.write(nodeSpec.getEnumType().getSimpleName());
        }
        else {
            out.write(nodeSpec.getJavaType().getJavaTypeClass().getSimpleName());
        }
    }


    /**
     * checks if the NodeSpec is completely suppressed from code generation.
     * @param nodeSpec
     * @return
     */
    protected boolean isCompletelySuppressed(NodeSpec nodeSpec) {
        if (nodeSpec.getSuppression() == null) {
            return false;
        }
        switch (nodeSpec.getSuppression()) {
            case ENTITY_CONFIGURATION:
                return true;
            case GENERATED_CODE:
                return true;
            case GENERATED_CODE_SETTER:
                return false;
            default:
                throw new IllegalStateException("Missing case for SuppressionSpec " + nodeSpec.getSuppression());
        }
    }

    protected String getJavaPath(String className) {
        return className.replace('.', '/');
    }

    protected boolean hasDifferentModelPackage(EntitySpec entitySpecA, EntitySpec entitySpecB) {
        return !getModelPackageName(entitySpecA).equals( getModelPackageName(entitySpecB));
    }

    protected String getModelPackageName(EntitySpec entitySpec) {
        int iA = entitySpec.getClassName().lastIndexOf('.');
        return entitySpec.getClassName().substring(0,  iA);
    }

    protected String getQueryPackageName(EntitySpec entitySpec) {
        int iA = entitySpec.getQueryClassName().lastIndexOf('.');
        return entitySpec.getQueryClassName().substring(0,  iA);
    }

    protected String getModelSimpleClassName(EntitySpec entitySpec) {
        int i = entitySpec.getClassName().lastIndexOf('.');
        return entitySpec.getClassName().substring(i+1);
    }

    protected String getQuerySimpleClassName(EntitySpec entitySpec) {
        int i = entitySpec.getQueryClassName().lastIndexOf('.');
        return entitySpec.getQueryClassName().substring(i+1);
    }





}
