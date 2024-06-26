package scott.barleydb.build;

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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.ddlgen.GenerateDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateMySqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GeneratePostgreSQLDatabaseScript;
import scott.barleydb.build.specification.modelgen.GenerateProxyModels;
import scott.barleydb.build.specification.modelgen.GenerateEnums;
import scott.barleydb.build.specification.modelgen.GenerateQueryModels;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;
import scott.barleydb.build.specification.staticspec.processor.StaticDefinitionProcessor;
import scott.barleydb.build.specification.vendor.MySqlSpecConverter;

/**
 * Generates all required artifacts from a DefinitionsSpec.
 * @author scott
 *
 */
public class Generator {

    public static void generate(StaticDefinitions definitions, String javaBasePath, String resourcesBasePath, boolean mysql) throws Exception {


        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        processor.process(definitions, registry);
        generate(registry, javaBasePath, resourcesBasePath, mysql, true);
    }

    public static void generate(SpecRegistry registry , String javaBasePath, String resourcesBasePath, boolean mysql, boolean generateScripts) throws Exception {
        for (DefinitionsSpec playSpec: registry.getDefinitions()) {

            int i = playSpec.getNamespace().lastIndexOf('.');
            final String name = playSpec.getNamespace().substring(i+1, playSpec.getNamespace().length());

            if (mysql) {
                playSpec = MySqlSpecConverter.convertSpec(playSpec);
            }

            JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            String basePackagePath = playSpec.getNamespace().replace('.', '/');

            File file = new File(resourcesBasePath + "/" + basePackagePath + "/schema/" + name + "-spec.xml");
            file.getParentFile().mkdirs();
            try (FileOutputStream fout = new FileOutputStream(file); ) {
                marshaller.marshal(registry, fout);
            }

            //GenerateDatabaseScript gen = new GenerateMySqlDatabaseScript();
            GenerateDatabaseScript gen = new GeneratePostgreSQLDatabaseScript();

            if (generateScripts) {
                try (Writer out = new FileWriter(resourcesBasePath + "/" + basePackagePath + "/schema/" + name + "-schema.sql")) {
                    out.write("/*\n--- Schema generated by Sort static definitions \n*/");
                    out.write(gen.generateScript(playSpec));
                    out.flush();
                }
            }

            GenerateEnums generateEnums = new GenerateEnums();
            generateEnums.generateEnums(javaBasePath, playSpec);

            GenerateProxyModels generateDataModels = new GenerateProxyModels();
            generateDataModels.generateDataModels(javaBasePath, playSpec);

            GenerateQueryModels generateQueryModels = new GenerateQueryModels();
            generateQueryModels.generateQueryModels(javaBasePath, playSpec);
        }
    }
}
