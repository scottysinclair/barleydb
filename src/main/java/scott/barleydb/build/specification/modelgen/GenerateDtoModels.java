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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SuppressionSpec;

public class GenerateDtoModels extends GenerateModelsHelper {

    public void generateDtoModels(String path, DefinitionsSpec definitions) throws IOException {
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            generateDto(path, definitions, entitySpec);
        }
    }

    private void generateDto(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        System.out.println("Generating dto class " + entitySpec.getClassName());
        File classFile = toFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getDtoPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            out.write("import scott.barleydb.api.dto.BaseDto;\n");
            out.write("\n");
            if (hasToManyReference(entitySpec)) {
                out.write("import scott.barleydb.api.dto.DtoList;\n");
                out.write("\n");
            }
            writeDtoImports(definitions, entitySpec, out);
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

            writeToString(out, entitySpec);

            out.write("}\n");
            out.flush();
        }
    }

    private void writeToString(Writer out, EntitySpec entitySpec) throws IOException {
      out.write("  public String toString() {\n");
      out.write("    return getClass().getSimpleName() + \"[");
      NodeSpec ns = entitySpec.getPrimaryKeyNodes(true).iterator().next();
      out.write(ns.getName());
      out.write(" = \" + " + ns.getName() + " + \"]\";\n");
      out.write("  }\n");
    }

    private String getDtoPackageName(EntitySpec entitySpec) {
      int iA = entitySpec.getClassName().lastIndexOf('.');
      return entitySpec.getClassName().substring(0,  iA).replace(".model", ".dto");
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
        out.write(getDtoSimpleClassName(entitySpec));
        out.write("() {\n");
        out.write("  }\n");
    }
    protected String getDtoSimpleClassName(EntitySpec entitySpec) {
      int i = entitySpec.getClassName().lastIndexOf('.');
      return entitySpec.getClassName().substring(i+1) + "Dto";
    }

    protected String getDtoClassName(EntitySpec entitySpec) {
      return (entitySpec.getClassName() + "Dto").replace(".model.", ".dto.");
    }


    private String calcNodeType(NodeSpec nodeSpec) {
      if (nodeSpec.getEnumSpec() != null) {
        return getSimpleEnumName(nodeSpec.getEnumSpec());
      }
      if (nodeSpec.getColumnName() != null) {
        if (nodeSpec.getRelation() == null) {
          return nodeSpec.getJavaType().getJavaTypeClass().getSimpleName();
        }
        else {
          return getDtoSimpleClassName( nodeSpec.getRelationSpec().getEntitySpec() );
        }
      }
      if (nodeSpec.getColumnName() == null){
        if (nodeSpec.getRelationSpec().getOwnwardJoin() != null) {
          NodeSpec oj = nodeSpec.getRelationSpec().getOwnwardJoin();
          return "DtoList<" + getDtoSimpleClassName(oj.getRelationSpec().getEntitySpec()) + ">";
        }
        else {
          return "DtoList<" + getDtoSimpleClassName(nodeSpec.getRelationSpec().getEntitySpec()) + ">";
        }
      }
      throw new IllegalStateException("Invalid node type");
    }

    private void writeDtoImports(DefinitionsSpec definitions, EntitySpec entitySpec, Writer out) throws IOException {
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
                    out.write(getDtoClassName( relationSpec.getEntitySpec() ));
                    out.write(";\n");
                }
            }
            if (nodeSpec.getEnumSpec() != null) {
                if (!writtenFirstNewLine) {
                    out.write("\n");
                    writtenFirstNewLine = true;
                }
                out.write("import ");
                out.write(nodeSpec.getEnumSpec().getClassName());
                out.write(";\n");
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
      out.write(calcNodeType(nodeSpec));
    }

    private void writeNodeGetterAndSetter(Writer out, DefinitionsSpec definitions, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getSuppression() == SuppressionSpec.DTO) {
          return;
        }
        out.write("\n");
        writeNodeGetter(out, definitions, nodeSpec);
//         if (nodeSpec.getSuppression() != SuppressionSpec.GENERATED_CODE_SETTER) {
             writeNodeSetter(out, nodeSpec);
  //       }
    }

    private void writeNodeSetter(Writer out, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getColumnName() != null) {
            out.write("\n");
            out.write("  public void ");
            out.write(toSetterName(nodeSpec));
            out.write("(");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(nodeSpec.getName());
            out.write(") {\n");
            out.write("    this.");
            out.write(nodeSpec.getName());
            out.write(" = ");
            out.write(nodeSpec.getName());
            out.write(";\n");
            out.write("  }\n");
        }
    }

    private void writeNodeGetter(Writer out, DefinitionsSpec definitions, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getColumnName() != null) {
            out.write("  public ");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(toGetterName(nodeSpec));
            out.write("() {\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(";\n");
            out.write("  }\n");
        }
        else if (nodeSpec.getRelation() != null) {
            out.write("  public ");
            writeJavaType(out, nodeSpec);
            out.write(" ");
            out.write(toGetterName(nodeSpec));
            out.write("() {\n");
            out.write("    return ");
            out.write(nodeSpec.getName());
            out.write(";\n");
            out.write("  }\n");
        }
    }

    private void writeClassDeclaration(Writer out, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        out.write("public class ");
        out.write(getModelSimpleClassName(entitySpec));
        out.write("Dto ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends ");
            out.write(getDtoSimpleClassName(entitySpec.getParentEntity()));
            out.write(" ");
        }
        else {
            out.write("extends BaseDto ");
        }
    }

    private void writeNodeFieldDeclarations(Writer out, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getSuppression() == SuppressionSpec.DTO) {
          return;
        }
        out.write("  private ");
        writeNodeFieldType(out, nodeSpec);
        out.write(" ");
        out.write(nodeSpec.getName());
        if (nodeSpec.getColumnName() != null) {
          out.write(";\n");
        }
        else if (nodeSpec.getRelation() != null) {
          out.write(" = new DtoList<>();\n");
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

    private boolean hasToManyReference(EntitySpec entitySpec) {
        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null && nodeSpec.getRelationSpec().getBackReference() != null) {
                return true;
            }
        }
        return false;
    }

    private File toFile(String path, EntitySpec entitySpec) {
        String name = entitySpec.getClassName() + "Dto";
        name = name.replace(".model.", ".dto.");
        return toFile(path, name);
    }
    private File toFile(String path, String className) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        int i = className.lastIndexOf('.');
        String name = className.substring(0, i);
        name += className.substring(i);
        return new File(path + getJavaPath(name) + ".java");
    }

    protected void writeJavaType(Writer out, NodeSpec nodeSpec) throws IOException {
      writeJavaType(out, nodeSpec, false);
  }

  protected void writeJavaType(Writer out, NodeSpec nodeSpec, boolean useFkType) throws IOException {
      RelationSpec relationSpec = nodeSpec.getRelationSpec();
      if (relationSpec != null) {
          NodeSpec ownwardJoin = nodeSpec.getRelationSpec().getOwnwardJoin();
          if (relationSpec.isForeignKeyRelation()) {
              if (ownwardJoin != null) {
                  out.write(getDtoSimpleClassName(ownwardJoin.getEntity()));
              }
              else {
                  if (useFkType) {
                      Collection<NodeSpec> pks = nodeSpec.getRelationSpec().getEntitySpec().getPrimaryKeyNodes(true);
                      out.write(pks.iterator().next().getJavaType().getJavaTypeClass().getSimpleName());
                  }
                  else {
                      out.write(getDtoSimpleClassName(relationSpec.getEntitySpec()));
                  }
              }
          }
          else {
              if (ownwardJoin != null) {
                  out.write("DtoList<");
                  out.write(getDtoSimpleClassName(ownwardJoin.getRelationSpec().getEntitySpec()));
                  out.write(">");
              }
              else {
                  out.write("DtoList<");
                  out.write(getDtoSimpleClassName(relationSpec.getEntitySpec()));
                  out.write(">");
              }
          }
      }
      else if (nodeSpec.getEnumSpec() != null) {
          out.write(nodeSpec.getEnumSpec().getClassName());
      }
      else {
          out.write(nodeSpec.getJavaType().getJavaTypeClass().getSimpleName());
      }
  }

}
