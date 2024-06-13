package scott.barleydb.test;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.example.acl.AclSpec;
import org.example.etl.EtlSpec;
import org.example.etl.model.StructureType;
import org.example.etl.model.SyntaxType;
import org.junit.Before;
import org.junit.Test;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.ddlgen.GenerateDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateHsqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateMySqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateOracleDatabaseScript;
import scott.barleydb.build.specification.graphql.GenerateGrapqlSDL;
import scott.barleydb.build.specification.modelgen.GenerateProxyModels;
import scott.barleydb.build.specification.modelgen.GenerateDtoModels;
import scott.barleydb.build.specification.modelgen.GenerateEnums;
import scott.barleydb.build.specification.modelgen.GenerateQueryModels;
import scott.barleydb.build.specification.staticspec.processor.StaticDefinitionProcessor;
import scott.barleydb.build.specification.vendor.MySqlSpecConverter;

/**
 * Tests generating an XML specification from a static definition
 * @author scott
 *
 */
public class TestGenerator {

    @Before
    public void setup() {
        File file = new File("target/generated/src/test/java/org/example/etl");
        file.mkdirs();
        file = new File("target/generated/src/test/java/org/example/acl");
        file.mkdirs();
        file = new File("target/generated/src/test/resources/");
        file.mkdirs();
    }

    @Test
    public void testGenerateAclXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        @SuppressWarnings("unused")
        DefinitionsSpec aclSpec = processor.process(new AclSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, EtlSpec.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(registry, System.out);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        marshaller.marshal(registry, bout);
        byte[] data1 = bout.toByteArray();

        Unmarshaller um = jc.createUnmarshaller();
        registry = (SpecRegistry)um.unmarshal(new ByteArrayInputStream(data1));

        bout = new ByteArrayOutputStream();
        marshaller.marshal(registry, bout);
        byte[] data2 = bout.toByteArray();
        assertTrue(Arrays.equals(data1, data2));


        FileOutputStream fout = new FileOutputStream(new File("target/generated/src/test/java/org/example/acl/aclspec.xml"));
        marshaller.marshal(registry, fout);
        fout.flush();
        fout.close();
    }

    @Test
    public void testGenerateGraphqlEtlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        @SuppressWarnings("unused")
        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);
        GenerateGrapqlSDL gen = new GenerateGrapqlSDL(registry, null);
        System.out.println(gen.createSdl());
    }

    @Test
    public void testGenerateEtlXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        @SuppressWarnings("unused")
        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        marshaller.marshal(registry, bout);
        byte[] data1 = bout.toByteArray();

        Unmarshaller um = jc.createUnmarshaller();
        registry = (SpecRegistry)um.unmarshal(new ByteArrayInputStream(data1));

        bout = new ByteArrayOutputStream();
        marshaller.marshal(registry, bout);
        byte[] data2 = bout.toByteArray();
        assertTrue(Arrays.equals(data1, data2));

        File file = new File("target/generated/src/test/java/org/example/etl/etlspec.xml");
        FileOutputStream fout = new FileOutputStream(file);
        marshaller.marshal(registry, fout);
        fout.flush();
        fout.close();
    }

    @Test
    public void generateDDLForHsqldb() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        DefinitionsSpec aclSpec = registry.getDefinitionsSpec("org.example.acl");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateScript(aclSpec));
        System.out.println();
        System.out.println(gen.generateScript(etlSpec));

        try ( Writer out = new FileWriter("target/generated/src/test/resources/hsqldb-schema.sql"); ) {
            out.write("---\n--- Schema generated by BarleyDB static definitions ---\n---\n---\n");
            out.write(gen.generateScript(aclSpec));
            out.write('\n');
            out.write(gen.generateScript(etlSpec));
            out.flush();
        }
    }

    @Test
    public void generateDDLForOracle() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        DefinitionsSpec aclSpec = registry.getDefinitionsSpec("org.example.acl");

        GenerateDatabaseScript gen = new GenerateOracleDatabaseScript();

        System.out.println(gen.generateScript(aclSpec));
        System.out.println();
        System.out.println(gen.generateScript(etlSpec));

        try ( Writer out = new FileWriter("target/generated/src/test/resources/oracle-schema.sql"); ) {
            out.write("---\n--- Schema generated by BarleyDB static definitions ---\n---\n---\n");
            out.write(gen.generateScript(aclSpec));
            out.write('\n');
            out.write(gen.generateScript(etlSpec));
            out.flush();
        }
    }

    @Test
    public void generateDDLForMySql() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        DefinitionsSpec aclSpec = registry.getDefinitionsSpec("org.example.acl");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateScript( MySqlSpecConverter.convertSpec(aclSpec)) );
        System.out.println();
        System.out.println(gen.generateScript( MySqlSpecConverter.convertSpec(etlSpec)) );

        try ( Writer out = new FileWriter("target/generated/src/test/resources/mysql-schema.sql"); ) {
            out.write("/*\n Schema generated by BarleyDB static definitions\n*/\n");
            out.write(gen.generateScript(MySqlSpecConverter.convertSpec(aclSpec)) );
            out.write('\n');
            out.write(gen.generateScript(MySqlSpecConverter.convertSpec(etlSpec)) );
            out.flush();
        }
    }

    @Test
    public void generateModels() throws IOException {
        generateAclModels();
        generateEtlModels();
    }


    @Test
    public void generateAclModels() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec aclSpec = processor.process(new AclSpec(), registry);

        GenerateEnums generateEnums = new GenerateEnums();
        generateEnums.generateEnums("target/generated/src/test/java", aclSpec);

        GenerateProxyModels generateModels = new GenerateProxyModels();
        generateModels.generateDataModels("target/generated/src/test/java", aclSpec);

        GenerateQueryModels generateQueryModels = new GenerateQueryModels();
        generateQueryModels.generateQueryModels("target/generated/src/test/java", aclSpec);

        GenerateDtoModels genDto = new GenerateDtoModels();
        genDto.generateDtoModels("target/generated/src/test/java", aclSpec);
    }

    @Test
    public void generateEtlModels() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        GenerateEnums generateEnums = new GenerateEnums();
        generateEnums.generateEnums("target/generated/src/test/java", etlSpec);

        GenerateProxyModels generateDataModels = new GenerateProxyModels();
        generateDataModels.generateDataModels("target/generated/src/test/java", etlSpec);

        GenerateQueryModels generateQueryModels = new GenerateQueryModels();
        generateQueryModels.generateQueryModels("target/generated/src/test/java", etlSpec);

        GenerateDtoModels genDto = new GenerateDtoModels();
        genDto.generateDtoModels("target/generated/src/test/java", etlSpec);
    }

    @Test
    public void generateCleanScript() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        DefinitionsSpec aclSpec = registry.getDefinitionsSpec("org.example.acl");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateCleanScript(etlSpec));
        System.out.println();
        System.out.println(gen.generateCleanScript(aclSpec));

        try ( Writer out = new FileWriter("target/generated/src/test/resources/clean.sql"); ) {
            out.write("/*\n Clean script generated by BarleyDB static definitions\n*/");
            out.write(gen.generateCleanScript(etlSpec));
            out.write('\n');
            out.write(gen.generateCleanScript(aclSpec));
            out.flush();
        }
    }

    @Test
    public void generateDropScript() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec etlSpec = processor.process(new EtlSpec(), registry);

        DefinitionsSpec aclSpec = registry.getDefinitionsSpec("org.example.acl");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateDropScript(etlSpec));
        System.out.println();
        System.out.println(gen.generateDropScript(aclSpec));

        try ( Writer out = new FileWriter("target/generated/src/test/resources/drop.sql"); ) {
            out.write("/*\n Clean script generated by BarleyDB static definitions\n*/");
            out.write(gen.generateDropScript(etlSpec));
            out.write('\n');
            out.write(gen.generateDropScript(aclSpec));
            out.flush();
        }
    }
//
//    private void deleteFiles(String string) {
//        File dir = new File(string);
//        if (!dir.exists()) {
//            return;
//        }
//        for (File file: dir.listFiles()) {
//            if (file.isFile()) {
//                file.delete();
//            }
//        }
//    }

}
