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

import org.junit.Test;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.persist.PersistAnalyser;
import scott.sort.api.persist.PersistRequest;
import scott.sort.server.jdbc.persist.DatabaseDataSet;

import com.smartstream.mac.model.User;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.model.XmlStructure;
import com.smartstream.mi.model.XmlSyntaxModel;
import com.smartstream.mi.types.SyntaxType;

public class TestDatabaseDataSet extends TestBase {

    @Test
    public void testDatabaseDataSet() throws Exception {
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
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root1");
        mapping.setTargetFieldName("target1");
        syntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root2");
        mapping.setTargetFieldName("target2");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax
        XmlSyntaxModel subSyntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel.setName("SubSyntaxModel - ooooh");
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);

        mapping.setSubSyntax(subSyntaxModel); //cool, lets do it

        //add another mapping to the root level syntax
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTargetFieldName("target3");
        syntaxModel.getMappings().add(mapping);

        //do the sub-syntax mappings
        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel);
        mapping.setXpath("sub1");
        mapping.setTargetFieldName("subtarget1");
        subSyntaxModel.getMappings().add(mapping);

        mapping = serverEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel);
        mapping.setXpath("sub2");
        mapping.setTargetFieldName("subtarget2");
        subSyntaxModel.getMappings().add(mapping);

        /*
        * persist it the old way to redo an analysis
        */
        serverEntityContext.persist(new PersistRequest().save(syntaxModel));

        PersistRequest request = new PersistRequest();
        request.save(syntaxModel);

        PersistAnalyser analyser = new PersistAnalyser(serverEntityContext);
        analyser.analyse(request);
        printAnalysis(analyser);

        DatabaseDataSet databaseDataSet = new DatabaseDataSet(serverEntityContext);
        databaseDataSet.loadEntities(analyser.getUpdateGroup(), analyser.getDeleteGroup(), analyser.getDependsOnGroup());
        assertEquals(8, countLoadedEntities(databaseDataSet.getOwnEntityContext()));
    }

    private int countLoadedEntities(EntityContext entityContext) {
        int count = 0;
        for (Entity entity : entityContext.getEntities()) {
            if (entity.isLoaded()) {
                count++;
            }
        }
        return count;
    }
}
