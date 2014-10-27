package scott.sort.test;

import org.junit.Test;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.persist.PersistRequest;

import com.smartstream.mac.model.User;
import com.smartstream.mi.model.SyntaxType;
import com.smartstream.mi.model.XMLMapping;
import com.smartstream.mi.model.XMLStructure;
import com.smartstream.mi.model.XMLSyntaxModel;

public class TestClientPersistence extends TestRemoteClientBase {

    private XMLSyntaxModel buildSyntax(EntityContext entityContext) {
        XMLSyntaxModel syntaxModel = entityContext.newModel(XMLSyntaxModel.class);
        syntaxModel.setName("Scott's Syntax");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);

        User user = entityContext.newModel(User.class);
        user.setName("Jimmy");

        syntaxModel.setUser(user);

        XMLStructure structure = entityContext.newModel(XMLStructure.class);
        structure.setName("scott's structure");
        syntaxModel.setStructure(structure);

        XMLMapping mapping = entityContext.newModel(XMLMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root1");
        mapping.setTarget("target1");
        syntaxModel.getMappings().add(mapping);

        mapping = entityContext.newModel(XMLMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root2");
        mapping.setTarget("target2");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax
        XMLSyntaxModel subSyntaxModel = entityContext.newModel(XMLSyntaxModel.class);
        subSyntaxModel.setName("SubSyntaxModel - ooooh");
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);

        mapping.setSubSyntaxModel(subSyntaxModel); //set the subsyntax on the mapping

        //add another mapping to the root level syntax
        mapping = entityContext.newModel(XMLMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTarget("target3");
        syntaxModel.getMappings().add(mapping);

        //do the sub-syntax mappings
        mapping = entityContext.newModel(XMLMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub1");
        mapping.setTarget("subtarget1");
        subSyntaxModel.getMappings().add(mapping);

        mapping = entityContext.newModel(XMLMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub2");
        mapping.setTarget("subtarget2");
        subSyntaxModel.getMappings().add(mapping);
        return syntaxModel;
    }

    @Test
    public void testPersistNewXMLSyntax() throws Exception {
        try {
            System.out.println("STARTING TEST testPersistNewXMLSyntax");
            XMLSyntaxModel syntaxModel = buildSyntax(clientEntityContext);
            print("", syntaxModel);
            PersistRequest request = new PersistRequest();
            request.save(syntaxModel);

            clientEntityContext.persist(request);

            System.out.println("-------------- PRINTING RESULT OF PERIST ------------------");
            print("", syntaxModel);
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }


}
