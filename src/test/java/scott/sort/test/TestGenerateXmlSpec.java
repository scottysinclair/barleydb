package scott.sort.test;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import com.smartstream.mac.MacSpec;
import com.smartstream.mi.MiSpec;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.SpecRegistry;
import scott.sort.build.specification.staticspec.processor.StaticDefinitionProcessor;

/**
 * Tests generating an XML specification from a static definition
 * @author scott
 *
 */
public class TestGenerateXmlSpec {

    @Test
    public void testGenerateMiXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        DefinitionsSpec miSpec = processor.process(new MiSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(registry, System.out);

        FileOutputStream fout = new FileOutputStream(new File("src/test/java/com/smartstream/mi/mispec.xml"));
        marshaller.marshal(miSpec, fout);
        fout.flush();
        fout.close();
    }

    @Test
    public void testGenerateMacXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        DefinitionsSpec macSpec = processor.process(new MacSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(registry, System.out);

        FileOutputStream fout = new FileOutputStream(new File("src/test/java/com/smartstream/mac/macspec.xml"));
        marshaller.marshal(macSpec, fout);
        fout.flush();
        fout.close();
    }

}
