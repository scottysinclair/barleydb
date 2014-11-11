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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import scott.sort.api.persist.PersistAnalyser;
import scott.sort.api.persist.PersistRequest;

import com.smartstream.mac.model.User;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.model.XmlStructure;
import com.smartstream.mi.model.XmlSyntaxModel;
import com.smartstream.mi.types.SyntaxType;

public class TestPersistAnalyser extends TestBase {

    @Test
    public void testXMLSyntax() throws Exception {
        XmlSyntaxModel syntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);

        User user = serverEntityContext.newModel(User.class);
        user.setName("Jimmy");

        syntaxModel.setUser(user);

        XmlStructure structure = serverEntityContext.newModel(XmlStructure.class);
        structure.setName("scott's structure");
        syntaxModel.setStructure(structure);

        XmlMapping mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root1");
        mapping.setTarget("target1");
        syntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root2");
        mapping.setTarget("target2");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax
        XmlSyntaxModel subSyntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel.setName("SubSyntaxModel - ooooh");
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);

        mapping.setSubSyntaxModel(subSyntaxModel); //cool, lets do it

        //add another mapping to the root level syntax
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTarget("target3");
        syntaxModel.getMappings().add(mapping);

        //do the sub-syntax mappings
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub1");
        mapping.setTarget("subtarget1");
        subSyntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub2");
        mapping.setTarget("subtarget2");
        subSyntaxModel.getMappings().add(mapping);

        PersistRequest request = new PersistRequest();
        request.save(syntaxModel);

        PersistAnalyser analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printAnalysis(analyser);
        System.out.println("Optimized copy pre create");
        printAnalysis(analyser.optimizedCopy());

        assertEquals(9, analyser.getCreateGroup().getEntities().size());
        assertTrue(analyser.getUpdateGroup().getEntities().isEmpty());
        assertTrue(analyser.getDeleteGroup().getEntities().isEmpty());

        serverEntityContext.persist(new PersistRequest().save(syntaxModel));

        analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printAnalysis(analyser);
        System.out.println("Optimized copy");
        printAnalysis(analyser.optimizedCopy());

        print("", syntaxModel);

        assertTrue(analyser.getCreateGroup().getEntities().isEmpty());
        assertEquals(7, analyser.getUpdateGroup().getEntities().size());
        assertTrue(analyser.getDeleteGroup().getEntities().isEmpty());

        syntaxModel.getMappings().remove(1); //delete the subsyntax mapping
        System.out.println("Deleted the subsyntax mapping!");

        analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printAnalysis(analyser);
        System.out.println("Optimized copy");
        printAnalysis(analyser.optimizedCopy());

        assertTrue(analyser.getCreateGroup().getEntities().isEmpty());
        assertEquals(3, analyser.getUpdateGroup().getEntities().size());
        assertEquals(4, analyser.getDeleteGroup().getEntities().size());
    }

    @Test
    public void testOptimizeXMLSyntaxUpdateOperations() throws Exception {
        XmlSyntaxModel syntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);

        User user = serverEntityContext.newModel(User.class);
        user.setName("Jimmy");

        syntaxModel.setUser(user);

        XmlStructure structure = serverEntityContext.newModel(XmlStructure.class);
        structure.setName("scott's structure");
        syntaxModel.setStructure(structure);

        XmlMapping mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root1");
        mapping.setTarget("target1");
        syntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root2");
        mapping.setTarget("target2");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax
        XmlSyntaxModel subSyntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel.setName("SubSyntaxModel - ooooh");
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);

        mapping.setSubSyntaxModel(subSyntaxModel); //cool, lets do it

        //add another mapping to the root level syntax
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTarget("target3");
        syntaxModel.getMappings().add(mapping);

        //do the sub-syntax mappings
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub1");
        mapping.setTarget("subtarget1");
        subSyntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntaxModel(subSyntaxModel);
        mapping.setXpath("sub2");
        mapping.setTarget("subtarget2");
        subSyntaxModel.getMappings().add(mapping);

        PersistRequest request = new PersistRequest();
        request.save(syntaxModel);

        PersistAnalyser analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printAnalysis(analyser);
        System.out.println("Optimized copy");
        printUpdateAnalysis(analyser.optimizedCopy());

        assertEquals(9, analyser.getCreateGroup().getEntities().size());
        assertTrue(analyser.getUpdateGroup().getEntities().isEmpty());
        assertTrue(analyser.getDeleteGroup().getEntities().isEmpty());

        /*
        * persist it the old way to redo an analysis
        */
        serverEntityContext.persist(new PersistRequest().save(syntaxModel));

        System.out.println("Deleted the root synax!");

        serverEntityContext.persist(new PersistRequest().delete(syntaxModel));

        analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printDeleteAnalysis(analyser);
        System.out.println("Optimized copy");
        printDeleteAnalysis(analyser.optimizedCopy());
    }

}
