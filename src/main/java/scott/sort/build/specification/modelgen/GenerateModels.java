package scott.sort.build.specification.modelgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;

public class GenerateModels {

    public void generateModels(String path, DefinitionsSpec definitions) throws IOException {
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
            out.write("import java.util.List;\n");
            out.write("\n");
            out.write("import scott.sort.api.core.entity.Entity;\n");
            out.write("import scott.sort.api.core.entity.ValueNode;\n");
            out.write("import scott.sort.api.core.entity.RefNode;\n");
            out.write("import scott.sort.api.core.entity.ToManyNode;\n");
            out.write("import scott.sort.api.core.proxy.AbstractCustomEntityProxy;\n");
            out.write("import scott.sort.api.core.proxy.RefNodeProxyHelper;\n");
            out.write("import scott.sort.api.core.proxy.ToManyNodeProxyHelper;\n");
            out.write("\n");
            writeModelImports(definitions, entitySpec, out);
            out.write("\n");
            out.write("\n");
            writeClassDeclaration(out, entitySpec);
            out.write("{\n");
            if (!entitySpec.getNodeSpecs().isEmpty()) {
                out.write("\n");
                for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                    writeNodeFieldDeclarations(out, nodeSpec);
                }
                out.write("\n");
                out.write("\n");
            }
            out.write("  public ");
            out.write(getSimpleClassName(entitySpec));
            out.write("(Entity entity) {\n");
            out.write("    super(entity);\n");
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                writeNodeFieldAssignment(out, nodeSpec);
            }
            out.write("  }\n");
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                writeNodeGetterAndSetter(out, nodeSpec);
            }
            out.write("}\n");
            out.flush();
        }
    }

    private void writeNodeFieldDeclarations(Writer out, NodeSpec nodeSpec) throws IOException {
        out.write("  private final ");
        writeNodeFieldType(out, nodeSpec);
        out.write(" ");
        out.write(nodeSpec.getName());
        out.write(";\n");
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
                 out.write("\n");
                 writeNodeSetter(out, nodeSpec);
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

    private void writeJavaType(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        if (relationSpec != null) {
            if (relationSpec.isForeignKeyRelation()) {
                out.write(getSimpleClassName(relationSpec.getEntitySpec()));
            }
            else {
                out.write("List<");
                out.write(getSimpleClassName(relationSpec.getEntitySpec()));
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

    private void writeClassDeclaration(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("public class ");
        out.write(getSimpleClassName(entitySpec));
        out.write(" ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends ");
            out.write(getSimpleClassName(entitySpec.getParentEntity()));
            out.write(" ");
        }
        else {
            out.write("extends AbstractCustomEntityProxy ");
        }
    }

    private String getSimpleClassName(EntitySpec entitySpec) {
        int i = entitySpec.getClassName().lastIndexOf('.');
        return entitySpec.getClassName().substring(i+1);
    }

    private File toFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(entitySpec.getClassName()) + ".java");
    }

    private void writeModelImports(DefinitionsSpec definitions, EntitySpec entitySpec, Writer out) throws IOException {
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null) {
                RelationSpec relationSpec = nodeSpec.getRelationSpec();
                if (hasDifferentPackage(entitySpec, relationSpec.getEntitySpec())) {
                    out.write("import ");
                    out.write(relationSpec.getEntitySpec().getClassName());
                    out.write(";\n");
                }
            }
            if (nodeSpec.getEnumType() != null) {
                out.write("import ");
                out.write(nodeSpec.getEnumType().getName());
                out.write(";\n");
            }
        }
        if (entitySpec.getParentEntity() != null) {
            if (hasDifferentPackage(entitySpec, entitySpec.getParentEntity())) {
                out.write("import ");
                out.write(entitySpec.getParentEntity().getClassName());
                out.write(";\n");
            }
        }
        out.write("\n");
    }

    private boolean hasDifferentPackage(EntitySpec entitySpecA, EntitySpec entitySpecB) {
        return !getPackageName(entitySpecA).equals( getPackageName(entitySpecB));
    }

    private String getPackageName(EntitySpec entitySpec) {
        int iA = entitySpec.getClassName().lastIndexOf('.');
        return entitySpec.getClassName().substring(0,  iA);
    }

    private String getJavaPath(String className) {
        return className.replace('.', '/');
    }
}
