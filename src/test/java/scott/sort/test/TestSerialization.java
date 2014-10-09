package scott.sort.test;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Ignore;
import org.junit.Test;

import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.persister.PersistRequest;
import scott.sort.server.jdbc.persister.Persister;

import com.smartstream.mac.model.User;
import com.smartstream.messaging.model.SyntaxType;
import com.smartstream.messaging.model.XMLMapping;
import com.smartstream.messaging.model.XMLStructure;
import com.smartstream.messaging.model.XMLSyntaxModel;

public class TestSerialization extends TestBase {

    private XMLSyntaxModel buildSyntax() {
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

    @Ignore
    @Test
    public void testPersistNewXMLSyntax() throws Exception {
        System.out.println("STARTING TEST testPersistNewXMLSyntax");
        XMLSyntaxModel syntaxModel = buildSyntax();
        print("", syntaxModel);
        PersistRequest request = new PersistRequest();

        PersistAnalyser analyser = new PersistAnalyser(entityContext);
        analyser.analyse(request);

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("/tmp/out.bin"));
        out.writeObject(analyser);
        out.flush();
        out.close();

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("/tmp/out.bin"))) {
            analyser = (PersistAnalyser) in.readObject();
        }

        request.save(syntaxModel);

        Persister persister = new Persister(env, namespace);
        persister.persist(request);

        System.out.println("-------------- PRINTING RESULT OF PERIST ------------------");
        print("", syntaxModel);

    }

}