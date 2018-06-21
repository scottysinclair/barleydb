package scott.barleydb.build.specification.modelgen;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 *       <scottysinclair@gmail.com>
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
import java.util.Iterator;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;

public class GenerateEnums extends GenerateModelsHelper {

    public void generateEnums(String path, DefinitionsSpec definitions) throws IOException {
        for (EnumSpec enumSpec: definitions.getEnumSpecs()) {
            generateEnum(path, definitions, enumSpec);
        }
    }

    private void generateEnum(String path, DefinitionsSpec definitions, EnumSpec enumSpec) throws IOException {
//        System.out.println("Generating enum class " + enumSpec.getClassName());
        File classFile = toFile(path, enumSpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getModelPackageName(enumSpec));
            out.write(";\n");
            out.write("\n");
            writeClassJavaDoc(out);
            writeEnumDeclaration(out, definitions, enumSpec);

            for (Iterator<EnumValueSpec> i = enumSpec.getEnumValues().iterator(); i.hasNext();) {
                EnumValueSpec enumValue = i.next();
                out.write("  ");
                out.write(enumValue.getName());
                if (i.hasNext()) {
                    out.write(", //has database key ");
                    out.write(enumValue.getId().toString());
                }
                else {
                    out.write("; //has database key ");
                    out.write(enumValue.getId().toString());
                }
                out.write("\n");
            }
            out.write("}\n");
        }

    }

    private void writeClassJavaDoc(Writer out) throws IOException {
        out.write("/**\n");
        out.write(" * Generated from Entity Specification\n");
        out.write(" *\n");
        out.write(" * @author ");
        out.write(System.getProperty("user.name"));
        out.write("\n");
        out.write(" */\n");
    }

    private void writeEnumDeclaration(Writer out, DefinitionsSpec definitions, EnumSpec enumSpec) throws IOException {
        out.write("public enum ");
        out.write(getModelSimpleClassName(enumSpec));
        out.write(" {\n");
    }

    protected String getModelSimpleClassName(EnumSpec enumSpec) {
        int i = enumSpec.getClassName().lastIndexOf('.');
        return enumSpec.getClassName().substring(i+1);
    }


    protected String getModelPackageName(EnumSpec enumSpec) {
        int iA = enumSpec.getClassName().lastIndexOf('.');
        return enumSpec.getClassName().substring(0,  iA);
    }


    private File toFile(String path, EnumSpec enumSpec) {
        return toFile(path, enumSpec.getClassName());
    }
    private File toFile(String path, String className) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(className) + ".java");
    }

}
