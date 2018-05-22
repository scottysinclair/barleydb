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

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.JoinTypeSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;

public class GenerateQueryModels extends GenerateModelsHelper {

    public void generateQueryModels(String path, DefinitionsSpec definitions) throws IOException {
        Set<EntitySpec> parentSpecs = new HashSet<>();
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            if (entitySpec.getParentEntity() != null) {
                parentSpecs.add( entitySpec.getParentEntity() );
            }
        }
        for (EntitySpec parentSpec: parentSpecs) {
            generateAbstractQueryModel(path, definitions, parentSpec);
        }
        for (EntitySpec parentSpec: parentSpecs) {
            generateQueryModel(path, definitions, parentSpec, true);
        }
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            if (!parentSpecs.contains(entitySpec)) {
                generateQueryModel(path, definitions, entitySpec, false);
            }
        }
    }

    private void generateAbstractQueryModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
//        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toAbstractQueryFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getQueryPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            out.write("import scott.barleydb.api.query.JoinType;\n");
            out.write("import scott.barleydb.api.query.QProperty;\n");
            out.write("import scott.barleydb.api.query.QueryObject;\n");
            writeModelImports(definitions, entitySpec, false, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeAbstractQueryClassDeclaration(out, entitySpec);
            out.write("{\n");
            out.write("  private static final long serialVersionUID = 1L;\n");

            writeAbstractQueryConstructors(out, "QAbstract" + getModelSimpleClassName(entitySpec), entitySpec);
            out.write("\n");
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (!isCompletelySuppressed(nodeSpec)) {
                    writeNodeMethods(out, nodeSpec);
                }
            }
            out.write("}");
         }
    }

    private void generateQueryModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec, boolean ownMethodsDefinedOnSuperQueryType) throws IOException {
//        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getQueryPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            if (!ownMethodsDefinedOnSuperQueryType) {
                out.write("import scott.barleydb.api.query.JoinType;\n");
                out.write("import scott.barleydb.api.query.QProperty;\n");
            }
            out.write("import scott.barleydb.api.query.QueryObject;\n");
            writeModelImports(definitions, entitySpec, ownMethodsDefinedOnSuperQueryType, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeClassDeclaration(out, entitySpec, ownMethodsDefinedOnSuperQueryType);
            out.write("{\n");
            out.write("  private static final long serialVersionUID = 1L;\n");

            writeConstructors(out, getQuerySimpleClassName(entitySpec), entitySpec);
            out.write("\n");
            if (!ownMethodsDefinedOnSuperQueryType) {
                for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                    if (!isCompletelySuppressed(nodeSpec)) {
                        writeNodeMethods(out, nodeSpec);
                    }
                }
            }
            out.write("}");
         }
    }

    private void writeModelImports(DefinitionsSpec definitions, EntitySpec entitySpec, boolean ownMethodsDefinedOnSuperQueryType, Writer out) throws IOException {
        out.write("import ");
        out.write(entitySpec.getClassName());
        out.write(";\n");

        Set<String> imports = new HashSet<String>();
        if (!ownMethodsDefinedOnSuperQueryType) {
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (nodeSpec.getRelationSpec() != null) {
                    RelationSpec relationSpec = nodeSpec.getRelationSpec();
                    if (imports.add(relationSpec.getEntitySpec().getQueryClassName())) {
                        out.write("import ");
                        out.write(relationSpec.getEntitySpec().getQueryClassName());
                        out.write(";\n");
                        imports.add(relationSpec.getEntitySpec().getQueryClassName());
                    }
                }
                if (nodeSpec.getEnumSpec() != null) {
                    if (imports.add(nodeSpec.getEnumSpec().getClassName())) {
                        out.write("import ");
                        out.write(nodeSpec.getEnumSpec().getClassName());
                        out.write(";\n");
                        imports.add(nodeSpec.getEnumSpec().getClassName());
                    }
                }
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


    private void writeAbstractQueryClassDeclaration(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("class QAbstract");
        out.write(getModelSimpleClassName(entitySpec));
        out.write("<T extends SyntaxModel, CHILD extends QAbstractSyntaxModel<T, CHILD>> ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends QAbstract");
            out.write(getModelSimpleClassName(entitySpec.getParentEntity()));
            out.write("<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write(", ");
            out.write(getQuerySimpleClassName(entitySpec));
            out.write("> ");
        }
        else {
            out.write("extends QueryObject<T>");
        }
    }

    private void writeClassDeclaration(Writer out, EntitySpec entitySpec, boolean ownMethodsDefinedOnSuperQueryType) throws IOException {
        out.write("public class ");
        out.write(getQuerySimpleClassName(entitySpec));
        out.write(" ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends QAbstract");
            out.write(getModelSimpleClassName(entitySpec.getParentEntity()));
            out.write("<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write(", ");
            out.write(getQuerySimpleClassName(entitySpec));
            out.write("> ");
        }
        else if (ownMethodsDefinedOnSuperQueryType) {
            out.write("extends QAbstract");
            out.write(getModelSimpleClassName(entitySpec));
            out.write("<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write(", ");
            out.write(getQuerySimpleClassName(entitySpec));
            out.write("> ");
        }
        else {
            out.write("extends QueryObject<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write("> ");
        }
    }

    private void writeAbstractQueryConstructors(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        writeNormalAbstractQueryConstructor(out, simpleClassName, entitySpec);
        out.write("\n");
        writeSubQueryAbstractQueryConstructor(out, simpleClassName, entitySpec);
    }

    private void writeNormalAbstractQueryConstructor(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        out.write("  protected ");
        out.write(simpleClassName);
        out.write("(Class<T> modelClass) {\n");
        out.write("    super(modelClass);");
        out.write("  }\n");
    }

    private void writeSubQueryAbstractQueryConstructor(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        out.write("  protected ");
        out.write(simpleClassName);
        out.write("(Class<T> modelClass, QueryObject<?> parent) {\n");
        out.write("    super(modelClass, parent);");
        out.write("  }\n");
    }


    private void writeConstructors(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        writeDefaultConstructor(out, simpleClassName, entitySpec);
        out.write("\n");
        writeSubQueryConstructor(out, simpleClassName, entitySpec);
    }

    private void writeDefaultConstructor(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(simpleClassName);
        out.write("() {\n");
        out.write("    super(");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(".class);\n");
        out.write("  }\n");
    }

    private void writeSubQueryConstructor(Writer out, String simpleClassName,EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(simpleClassName);
        out.write("(QueryObject<?> parent) {\n");
        out.write("    super(");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(".class, parent);\n");
        out.write("  }\n");
    }

    private void writeNodeMethods(Writer out, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getRelationSpec() == null) {
            /*
             * this is a simple node so we write the QProperty
             */
            writeQPropertyMethod(out, nodeSpec, false);
        }
        else {
            //if we have a column and a relation (fk relation)
            //write the qproperty so we can query directly on the foreign key.
            if (nodeSpec.getColumnName() != null) {
                //we can only have FK properties if the entity on the other side has a single primary key
                if (nodeSpec.getRelationSpec().getEntitySpec().getPrimaryKeyNodes(true).size() == 1) {
                    writeQPropertyMethod(out, nodeSpec, true);
                }
            }

            writeJoinMethod(out, nodeSpec);
            /*
             * TODO: No time to add exists methods for exists across join tables.
             */
            if (nodeSpec.getRelationSpec().getOwnwardJoin() == null) {
                writeExistsMethod(out, nodeSpec);
            }
        }
    }

    private void writeJoinMethod(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        if (nodeSpec.getRelationSpec().getOwnwardJoin() == null) {
            out.write("\n  public ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(getJoinToMethodName(nodeSpec));
            out.write("() {\n");
            out.write("    ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(" = new ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write("();\n");
            out.write("    ");
            writeAddJoinMethodName(out, nodeSpec);
            out.write("(");
            out.write(nodeSpec.getName());
            out.write(", \"");
            out.write(nodeSpec.getName());
            out.write("\");\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(";\n");
            out.write("  }\n");

            out.write("\n  public ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(getJoinToMethodName(nodeSpec));
            out.write("(JoinType joinType) {\n");
            out.write("    ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(" = new ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write("();\n");
            out.write("    addJoin");
            out.write("(");
            out.write(nodeSpec.getName());
            out.write(", \"");
            out.write(nodeSpec.getName());
            out.write("\", joinType);\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(";\n");
            out.write("  }\n");
        }
        else {
            /*
             * We are writing the query method which joins again across the join table.
             */
            NodeSpec ownwardJoin = nodeSpec.getRelationSpec().getOwnwardJoin();
            EntitySpec targetEntity = ownwardJoin.getRelationSpec().getEntitySpec();

            out.write("\n  public ");
            out.write(getQuerySimpleClassName(targetEntity));
            out.write(" ");
            out.write(getJoinToMethodName(ownwardJoin));
            out.write("() {\n");
            out.write("    ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(" = new ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write("();\n");
            out.write("    ");
            writeAddJoinMethodName(out, nodeSpec);
            out.write("(");
            out.write(nodeSpec.getName());
            out.write(", \"");
            out.write(nodeSpec.getName());
            out.write("\");\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(".");
            out.write(getJoinToMethodName(ownwardJoin));
            out.write("();\n");
            out.write("  }\n");

            out.write("\n  public ");
            out.write(getQuerySimpleClassName(targetEntity));
            out.write(" ");
            out.write(getJoinToMethodName(ownwardJoin));
            out.write("(JoinType joinType) {\n");
            out.write("    ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(" = new ");
            out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
            out.write("();\n");
            out.write("    addJoin(");
            out.write(nodeSpec.getName());
            out.write(", \"");
            out.write(nodeSpec.getName());
            out.write("\", joinType);\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(".");
            out.write(getJoinToMethodName(ownwardJoin));
            out.write("(joinType);\n");
            out.write("  }\n");


        }
    }

    private void writeExistsMethod(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        out.write("\n  public ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(getExistsMethodName(nodeSpec));
        out.write("() {\n");
        out.write("    ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(nodeSpec.getName());
        out.write(" = new ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write("(this);\n");
        out.write("    addExists(");
        out.write(nodeSpec.getName());
        out.write(", \"");
        out.write(nodeSpec.getName());
        out.write("\");\n");
        out.write("    return ");
        out.write(nodeSpec.getName());
        out.write(";\n");
        out.write("  }\n");
    }

    private void writeQPropertyMethod(Writer out, NodeSpec nodeSpec, boolean useFkType) throws IOException {
        out.write("\n  public QProperty<");
        writeJavaType(out, nodeSpec, useFkType);
        out.write("> ");
        out.write(nodeSpec.getName());
        if (useFkType) {
            out.write("Id");
        }
        out.write("() {\n");
        out.write("    return new QProperty<");
        writeJavaType(out, nodeSpec, useFkType);
        out.write(">(this, \"");
        out.write(nodeSpec.getName());
        out.write("\");\n");
        out.write("  }\n");
    }

    private void writeAddJoinMethodName(Writer out, NodeSpec nodeSpec) throws IOException {
        JoinTypeSpec joinTypeSpec = nodeSpec.getRelationSpec().getJoinType();
        switch (joinTypeSpec) {
            case INNER_JOIN:
                out.write("addInnerJoin");
                break;
            case LEFT_OUTER_JOIN:
                out.write("addLeftOuterJoin");
                break;
            default:
                throw new IllegalStateException("Missing case for join type spec " + joinTypeSpec);
        }
    }

    private String getJoinToMethodName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "joinTo" + fc + name.substring(1, name.length());
    }

    private String getExistsMethodName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "exists" + fc + name.substring(1, name.length());
    }

    private File toAbstractQueryFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        File file = new File(path + getJavaPath(entitySpec.getQueryClassName()));
        File dir = file.getParentFile();
        return new File(dir, "QAbstract" + getModelSimpleClassName(entitySpec) + ".java");
    }

    private File toFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(entitySpec.getQueryClassName()) + ".java");
    }


}
