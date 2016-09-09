package scott.barleydb.build.specification.modelgen;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SuppressionSpec;

public class GenerateDataModels extends GenerateModelsHelper {

    public void generateDataModels(String path, DefinitionsSpec definitions) throws IOException {
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            generateModel(path, definitions, entitySpec);
        }
        generateProxyFactory(path, definitions);
    }

    private void generateProxyFactory(String path, DefinitionsSpec definitions) throws IOException {
        String proxyFactoryName = getProxyFactoryName(definitions);

        String packageName = getModelPackageName(definitions.getEntitySpecs().iterator().next());

        File classFile = toFile(path, packageName + "." + proxyFactoryName);
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(packageName);
            out.write(";\n");

            out.write("import scott.barleydb.api.core.entity.Entity;\n");
            out.write("import scott.barleydb.api.core.proxy.ProxyFactory;\n");
            out.write("import scott.barleydb.api.exception.model.ProxyCreationException;\n");

            out.write("\n");
            out.write("public class ");
            out.write(proxyFactoryName);
            out.write(" implements ProxyFactory {\n\n");
            out.write("  private static final long serialVersionUID = 1L;\n\n");
            out.write("  @SuppressWarnings(\"unchecked\")\n");
            out.write("  public <T> T newProxy(Entity entity) throws ProxyCreationException {\n");
            for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
                out.write("    if (entity.getEntityType().getInterfaceName().equals(");
                out.write(getModelSimpleClassName(entitySpec));
                out.write(".class.getName())) {\n");
                out.write("      return (T) new ");
                out.write(getModelSimpleClassName(entitySpec));
                out.write("(entity);\n");
                out.write("    }\n");
            }
            /*
            out.write("    try {\n");
            out.write("      return EntityProxy.generateProxy(getClass().getClassLoader(), entity);\n");
            out.write("    }\n");
            out.write("    catch (ClassNotFoundException x) {\n");
            out.write("      throw new ProxyCreationException(\"Could not generate dynamic proxy\", x);\n");
            out.write("    }\n");
            */
            out.write("    return null;\n");
            out.write("  }\n");
            out.write("}\n");

        }



    }

    private String getProxyFactoryName(DefinitionsSpec definitions) {
        String name = definitions.getNamespace();
        name = name.substring(name.lastIndexOf('.')+1, name.length());
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return name + "ProxyFactory";
    }

    private void generateModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getModelPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            if (hasToManyReference(entitySpec)) {
                out.write("import java.util.List;\n");
                out.write("\n");
            }
            out.write("import scott.barleydb.api.core.entity.Entity;\n");
            out.write("import scott.barleydb.api.core.entity.ValueNode;\n");
            if (entitySpec.getParentEntity() == null) {
                out.write("import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;\n");
            }
            if (hasFkReference(entitySpec)) {
                out.write("import scott.barleydb.api.core.entity.RefNode;\n");
                out.write("import scott.barleydb.api.core.proxy.RefNodeProxyHelper;\n");
            }
            if (hasToManyReference(entitySpec)) {
                out.write("import scott.barleydb.api.core.entity.ToManyNode;\n");
                out.write("import scott.barleydb.api.core.proxy.ToManyNodeProxyHelper;\n");
            }
            writeModelImports(definitions, entitySpec, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeClassDeclaration(out, definitions, entitySpec);
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

            writeConstructor(out, definitions, entitySpec);

            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (!isCompletelySuppressed(nodeSpec)) {
                    writeNodeGetterAndSetter(out, definitions, nodeSpec);
                }
            }
            out.write("}\n");
            out.flush();
        }
    }

    private void writeClassJavaDoc(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("/**\n");
        out.write(" * Generated from Entity Specification\n");
        out.write(" *\n");
        out.write(" * @author ");
        out.write(System.getProperty("user.name"));
        out.write("\n");
        out.write(" */\n");
    }

    private void writeConstructor(Writer out, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(getModelSimpleClassName(entitySpec));
        out.write("(Entity entity) {\n");
        out.write("    super(entity);\n");
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (!isCompletelySuppressed(nodeSpec)) {
                writeNodeFieldAssignment(out, nodeSpec, false);
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

    private void writeModelImports(DefinitionsSpec definitions, EntitySpec entitySpec, Writer out) throws IOException {
        Set<String> imports = new HashSet<>();
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
            /*
             * enums are now in the same package as data models.
            if (nodeSpec.getEnumSpec().getClassName() != null) {
                if (!writtenFirstNewLine) {
                    out.write("\n");
                    writtenFirstNewLine = true;
                }
                out.write("import ");
                out.write(nodeSpec.getEnumSpec().getClassName());
                out.write(";\n");
            }
            */
            if (nodeSpec.getJavaType() != null && nodeSpec.getJavaType().getJavaTypeClass() != null) {
                if (!nodeSpec.getJavaType().getJavaTypeClass().getName().startsWith("java.lang")) {
                    if (!nodeSpec.getJavaType().getJavaTypeClass().isArray()) {
                        if (imports.add(nodeSpec.getJavaType().getJavaTypeClass().getName())) {
                            out.write("import ");
                            out.write(nodeSpec.getJavaType().getJavaTypeClass().getName());
                            out.write(";\n");
                            imports.add(nodeSpec.getJavaType().getJavaTypeClass().getName());
                        }
                    }
                }
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

    private void writeNodeFieldAssignment(Writer out, NodeSpec nodeSpec, boolean toNull) throws IOException {
        out.write("    ");
        if (toNull){
            out.write(nodeSpec.getName());
            out.write(" = null;\n");
            return;
        }
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

    private void writeNodeGetterAndSetter(Writer out, DefinitionsSpec definitions, NodeSpec nodeSpec) throws IOException {
        switch(calcNodeType(nodeSpec)) {
            case "ToManyNode":
                out.write("\n");
                writeNodeGetter(out, definitions, nodeSpec);
                if (nodeSpec.getSuppression() != SuppressionSpec.GENERATED_CODE_SETTER) {
                    out.write("\n");
                    writeNodeSetter(out, nodeSpec);
                }
                break;
             default:
                 out.write("\n");
                 writeNodeGetter(out, definitions, nodeSpec);
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
            out.write("  public void ");
            out.write(toSetterName(nodeSpec));
            out.write("(");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(") {\n");
            out.write("    this.");
            out.write(nodeSpec.getName());
            out.write(".toManyNode.clear();\n");
            out.write("     for (" + nodeSpec.getRelationSpec().getEntitySpec().getClassName() + " item: " + nodeSpec.getName() + ") {\n");
            out.write("          super.getListProxy(this." + nodeSpec.getName() + ".toManyNode).add( item );\n");
            out.write("     }\n");
            out.write("  }\n");
            break;
        }
    }

    private void writeNodeGetter(Writer out, DefinitionsSpec definitions, NodeSpec nodeSpec) throws IOException {
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

    private void writeClassDeclaration(Writer out, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
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
        return toFile(path, entitySpec.getClassName());
    }
    private File toFile(String path, String className) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(className) + ".java");
    }

}
