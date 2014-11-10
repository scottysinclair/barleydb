package scott.sort.test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import com.smartstream.mi.MiSpec;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;

import scott.sort.api.config.Definitions;
import scott.sort.api.specification.SpecRegistry;
import scott.sort.build.specification.staticspec.processor.StaticDefinitionProcessor;

/**
 * Tests generating an XML specification from a static definition
 * @author scott
 *
 */
public class TestGenerateXmlSpec {

    @Test
    public void testGenerateFullXmlSpec() throws Exception {
        SpecRegistry registry = new SpecRegistry();
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        processor.process(new MiSpec(), registry);

        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(registry, System.out);
    }

}
