package scott.sort.build.specification.modelgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;
import scott.sort.api.specification.SuppressionSpec;

public class GenerateDataModels extends GenerateModelsHelper {

    public void generateDataModels(String path, DefinitionsSpec definitions) throws IOException {
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            generateModel(path, definitions, entitySpec);
        }
    }

    private void generateModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            if (hasToManyReference(entitySpec)) {
                out.write("import java.util.List;\n");
                out.write("\n");
            }
            out.write("import scott.sort.api.core.entity.Entity;\n");
            out.write("import scott.sort.api.core.entity.ValueNode;\n");
            if (entitySpec.getParentEntity() == null) {
                out.write("import scott.sort.api.core.proxy.AbstractCustomEntityProxy;\n");
            }
            if (hasFkReference(entitySpec)) {
                out.write("import scott.sort.api.core.entity.RefNode;\n");
                out.write("import scott.sort.api.core.proxy.RefNodeProxyHelper;\n");
            }
            if (hasToManyReference(entitySpec)) {
                out.write("import scott.sort.api.core.entity.ToManyNode;\n");
                out.write("import scott.sort.api.core.proxy.ToManyNodeProxyHelper;\n");
            }
            writeModelImports(definitions, entitySpec, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeClassDeclaration(out, entitySpec);
            out.write("{\n");
            out.write("  private static final long serialVersionUID = 1L;\n");
            if (!entitySpec.getNodeSpecs().isEmpty()) {
                out.write("\n");
                for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                    if (!isCompletelySuppressed(nodeSpec)) {
                        writeNodeFieldDeclarations(out, nodeSpec);
                    }
                }
                out.write("\n");
            }

            writeConstructor(out, entitySpec);

            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (!isCompletelySuppressed(nodeSpec)) {
                    writeNodeGetterAndSetter(out, nodeSpec);
                }
            }
            out.write("}\n");
            out.flush();
        }
    }

    private void writeClassJavaDoc(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("/**\n");
        out.write(" * Generated from Entity Specification on ");
        out.write(new Date().toString());
        out.write("\n");
        out.write(" *\n");
        out.write(" * @author ");
        out.write(System.getProperty("user.name"));
        out.write("\n");
        out.write(" */\n");
    }

    private void writeConstructor(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(getModelSimpleClassName(entitySpec));
        out.write("(Entity entity) {\n");
        out.write("    super(entity);\n");
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (!isCompletelySuppressed(nodeSpec)) {
                writeNodeFieldAssignment(out, nodeSpec);
            }
        }
        out.write("  }\n");
    }

    private String calcNodeType(NodeSpec nodeSpec) {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        if (relationSpec != null) {
            if (relationSpec.isForeignKeyRelation()) {
                return RefNode.class.getSimpleName();
            }
            else if (relationSpec.getBackReference() != null) {
                return ToManyNode.class.getSimpleName();
            }
            else {
                throw new IllegalStateException("Invalid relationSpec");
            }
        }
        else {
            return ValueNode.class.getSimpleName();
        }
    }

    private void writeNodeFieldType(Writer out, NodeSpec nodeSpec) throws IOException {
        switch (calcNodeType(nodeSpec)) {
            case "RefNode":
                out.write("RefNodeProxyHelper");
                return;
            case "ToManyNode":
                out.write("ToManyNodeProxyHelper");
                return;
            case "ValueNode":
                out.write("ValueNode");
                return;
        }
    }

    private void writeNodeFieldAssignment(Writer out, NodeSpec nodeSpec) throws IOException {
        out.write("    ");
        switch(calcNodeType(nodeSpec)) {
            case "ValueNode":
                out.write(nodeSpec.getName());
                out.write(" = entity.getChild(\"");
                out.write(nodeSpec.getName());
                out.write("\", ValueNode.class, true);\n");
                break;
            case "RefNode":
                out.write(nodeSpec.getName());
                out.write(" = new RefNodeProxyHelper(entity.getChild(\"");
                out.write(nodeSpec.getName());
                out.write("\", RefNode.class, true));\n");
                break;
            case "ToManyNode":
                out.write(nodeSpec.getName());
                out.write(" = new ToManyNodeProxyHelper(entity.getChild(\"");
                out.write(nodeSpec.getName());
                out.write("\", ToManyNode.class, true));\n");
                break;
            }
    }

    private void writeNodeGetterAndSetter(Writer out, NodeSpec nodeSpec) throws IOException {
        switch(calcNodeType(nodeSpec)) {
            case "ToManyNode":
                out.write("\n");
                writeNodeGetter(out, nodeSpec);
                break;
             default:
                 out.write("\n");
                 writeNodeGetter(out, nodeSpec);
                 if (nodeSpec.getSuppression() != SuppressionSpec.GENERATED_CODE_SETTER) {
                     out.write("\n");
                     writeNodeSetter(out, nodeSpec);
                 }
        }
    }

    private void writeNodeSetter(Writer out, NodeSpec nodeSpec) throws IOException {
        switch(calcNodeType(nodeSpec)) {
        case "ValueNode":
            out.write("  public void ");
            out.write(toSetterName(nodeSpec));
            out.write("(");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(") {\n");
            out.write("    this.");
            out.write(nodeSpec.getName());
            out.write(".setValue(");
            out.write(nodeSpec.getName());
            out.write(");\n");
            out.write("  }\n");
            break;
        case "RefNode":
            out.write("  public void ");
            out.write(toSetterName(nodeSpec));
            out.write("(");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(") {\n");
            out.write("    setToRefNode(this.");
            out.write(nodeSpec.getName());
            out.write(".refNode, ");
            out.write(nodeSpec.getName());
            out.write(");\n");
            out.write("  }\n");
            break;
        case "ToManyNode":
            break;
        }
    }

    private void writeNodeGetter(Writer out, NodeSpec nodeSpec) throws IOException {
        switch(calcNodeType(nodeSpec)) {
        case "ValueNode":
            out.write("  public ");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(toGetterName(nodeSpec));
            out.write("() {\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(".getValue();\n");
            out.write("  }\n");
            break;
        case "RefNode":
            out.write("  public ");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(toGetterName(nodeSpec));
            out.write("() {\n");
            out.write("    return super.getFromRefNode(");
            out.write(nodeSpec.getName());
            out.write(".refNode);\n");
            out.write("  }\n");
            break;
        case "ToManyNode":
            out.write("  public ");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(toGetterName(nodeSpec));
            out.write("() {\n");
            out.write("    return super.getListProxy(");
            out.write(nodeSpec.getName());
            out.write(".toManyNode);\n");
            out.write("  }\n");
            break;
        }
    }

    private void writeClassDeclaration(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("public class ");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(" ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends ");
            out.write(getModelSimpleClassName(entitySpec.getParentEntity()));
            out.write(" ");
        }
        else {
            out.write("extends AbstractCustomEntityProxy ");
        }
    }

    private void writeNodeFieldDeclarations(Writer out, NodeSpec nodeSpec) throws IOException {
        out.write("  private final ");
        writeNodeFieldType(out, nodeSpec);
        out.write(" ");
        out.write(nodeSpec.getName());
        out.write(";\n");
    }

    private String toGetterName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "get" + fc + name.substring(1, name.length());
    }

    private String toSetterName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "set" + fc + name.substring(1, name.length());
    }

    private boolean hasToManyReference(EntitySpec entitySpec) {
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null && nodeSpec.getRelationSpec().getBackReference() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFkReference(EntitySpec entitySpec) {
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null && nodeSpec.getRelationSpec().isForeignKeyRelation()) {
                return true;
            }
        }
        return false;
    }

    private File toFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(entitySpec.getClassName()) + ".java");
    }

}
