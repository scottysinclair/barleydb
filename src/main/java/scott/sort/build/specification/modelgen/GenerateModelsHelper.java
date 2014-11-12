package scott.sort.build.specification.modelgen;

import java.io.IOException;
import java.io.Writer;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;

public class GenerateModelsHelper {

    protected void writeModelImports(DefinitionsSpec definitions, EntitySpec entitySpec, Writer out) throws IOException {
        boolean writtenFirstNewLine = false;
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null) {
                RelationSpec relationSpec = nodeSpec.getRelationSpec();
                if (hasDifferentModelPackage(entitySpec, relationSpec.getEntitySpec())) {
                    if (!writtenFirstNewLine) {
                        out.write("\n");
                        writtenFirstNewLine = true;
                    }
                    out.write("import ");
                    out.write(relationSpec.getEntitySpec().getClassName());
                    out.write(";\n");
                }
            }
            if (nodeSpec.getEnumType() != null) {
                if (!writtenFirstNewLine) {
                    out.write("\n");
                    writtenFirstNewLine = true;
                }
                out.write("import ");
                out.write(nodeSpec.getEnumType().getName());
                out.write(";\n");
            }
        }
        if (entitySpec.getParentEntity() != null) {
            if (hasDifferentModelPackage(entitySpec, entitySpec.getParentEntity())) {
                if (!writtenFirstNewLine) {
                    out.write("\n");
                    writtenFirstNewLine = true;
                }
                out.write("import ");
                out.write(entitySpec.getParentEntity().getClassName());
                out.write(";\n");
            }
        }
    }

    protected void writeJavaType(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        if (relationSpec != null) {
            if (relationSpec.isForeignKeyRelation()) {
                out.write(getModelSimpleClassName(relationSpec.getEntitySpec()));
            }
            else {
                out.write("List<");
                out.write(getModelSimpleClassName(relationSpec.getEntitySpec()));
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
        return !getPackageName(entitySpecA).equals( getPackageName(entitySpecB));
    }

    protected String getPackageName(EntitySpec entitySpec) {
        int iA = entitySpec.getClassName().lastIndexOf('.');
        return entitySpec.getClassName().substring(0,  iA);
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
