package scott.sort.test;

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

import org.junit.Ignore;
import org.junit.Test;

import com.smartstream.mac.MacSpec;
import com.smartstream.mi.MiSpec;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.SpecRegistry;
import scott.sort.build.specification.ddlgen.GenerateDatabaseScript;
import scott.sort.build.specification.ddlgen.GenerateHsqlDatabaseScript;
import scott.sort.build.specification.ddlgen.GenerateMySqlDatabaseScript;
import scott.sort.build.specification.modelgen.GenerateDataModels;
import scott.sort.build.specification.modelgen.GenerateQueryModels;
import scott.sort.build.specification.staticspec.processor.StaticDefinitionProcessor;
import scott.sort.build.specification.vendor.MySqlSpecConverter;

/**
 * Tests generating an XML specification from a static definition
 * @author scott
 *
 */
public class TestGenerator {

    @Test
    public void testGenerateMacXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        @SuppressWarnings("unused")
        DefinitionsSpec macSpec = processor.process(new MacSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
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


        FileOutputStream fout = new FileOutputStream(new File("src/test/java/com/smartstream/mac/macspec.xml"));
        marshaller.marshal(registry, fout);
        fout.flush();
        fout.close();
    }

    @Test
    public void testGenerateMiXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        @SuppressWarnings("unused")
        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
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

        File file = new File("src/test/java/com/smartstream/mi/mispec.xml");
        FileOutputStream fout = new FileOutputStream(file);
        marshaller.marshal(registry, fout);
        fout.flush();
        fout.close();
    }

    @Test
    public void generateDDLForHsqldb() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        DefinitionsSpec macSpec = registry.getDefinitionsSpec("com.smartstream.mac");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateScript(macSpec));
        System.out.println();
        System.out.println(gen.generateScript(miSpec));

        try ( Writer out = new FileWriter("src/test/resources/hsqldb-schema.sql"); ) {
            out.write("---\n--- Schema generated by Sort static definitions ---\n---\n---\n");
            out.write(gen.generateScript(macSpec));
            out.write('\n');
            out.write(gen.generateScript(miSpec));
            out.flush();
        }
    }

    @Ignore
    @Test
    public void generateDDLForMySql() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        DefinitionsSpec macSpec = registry.getDefinitionsSpec("com.smartstream.mac");

        GenerateDatabaseScript gen = new GenerateMySqlDatabaseScript();

        System.out.println(gen.generateScript( MySqlSpecConverter.convertSpec(macSpec)) );
        System.out.println();
        System.out.println(gen.generateScript( MySqlSpecConverter.convertSpec(miSpec)) );

        try ( Writer out = new FileWriter("src/test/resources/mysql-schema.sql"); ) {
            out.write("/*\n Schema generated by Sort static definitions\n*/\n");
            out.write(gen.generateScript(MySqlSpecConverter.convertSpec(macSpec)) );
            out.write('\n');
            out.write(gen.generateScript(MySqlSpecConverter.convertSpec(miSpec)) );
            out.flush();
        }
    }

    @Test
    public void generateModels() throws IOException {
        generateMacModels();
        generateMiModels();
    }


    @Test
    public void generateMacModels() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec macSpec = processor.process(new MacSpec(), registry);

        deleteFiles("src/test/java/com/smartstream/mac/model");

        GenerateDataModels generateModels = new GenerateDataModels();
        generateModels.generateDataModels("src/test/java", macSpec);

        deleteFiles("src/test/java/com/smartstream/mac/query");
        GenerateQueryModels generateQueryModels = new GenerateQueryModels();
        generateQueryModels.generateQueryModels("src/test/java", macSpec);
}

    @Test
    public void generateMiModels() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        deleteFiles("src/test/java/com/smartstream/mi/model");
        GenerateDataModels generateDataModels = new GenerateDataModels();
        generateDataModels.generateDataModels("src/test/java", miSpec);

        deleteFiles("src/test/java/com/smartstream/mi/query");
        GenerateQueryModels generateQueryModels = new GenerateQueryModels();
        generateQueryModels.generateQueryModels("src/test/java", miSpec);
    }

    @Test
    public void generateCleanScript() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        DefinitionsSpec macSpec = registry.getDefinitionsSpec("com.smartstream.mac");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateCleanScript(miSpec));
        System.out.println();
        System.out.println(gen.generateCleanScript(macSpec));

        try ( Writer out = new FileWriter("src/test/resources/clean.sql"); ) {
            out.write("/*\n Clean script generated by Sort static definitions\n*/");
            out.write(gen.generateCleanScript(miSpec));
            out.write('\n');
            out.write(gen.generateCleanScript(macSpec));
            out.flush();
        }
    }

    @Test
    public void generateDropScript() throws IOException {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();

        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        DefinitionsSpec macSpec = registry.getDefinitionsSpec("com.smartstream.mac");

        GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();

        System.out.println(gen.generateDropScript(miSpec));
        System.out.println();
        System.out.println(gen.generateDropScript(macSpec));

        try ( Writer out = new FileWriter("src/test/resources/drop.sql"); ) {
            out.write("/*\n Clean script generated by Sort static definitions\n*/");
            out.write(gen.generateDropScript(miSpec));
            out.write('\n');
            out.write(gen.generateDropScript(macSpec));
            out.flush();
        }
    }

    private void deleteFiles(String string) {
        File dir = new File(string);
        if (!dir.exists()) {
            return;
        }
        for (File file: dir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

}
