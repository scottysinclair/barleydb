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

import org.junit.Test;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.server.jdbc.persister.*;
import static org.junit.Assert.*;

import com.smartstream.mac.model.User;
import com.smartstream.messaging.model.*;

public class TestDatabaseDataSet extends TestBase {

    @Test
    public void testDatabaseDataSet() throws Exception {
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

        mapping.setSubSyntaxModel(subSyntaxModel); //cool, lets do it

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

        /*
        * persist it the old way to redo an analysis
        */
        entityContext.persist(new PersistRequest().save(syntaxModel));

        PersistRequest request = new PersistRequest();
        request.save(syntaxModel);

        PersistAnalyser analyser = new PersistAnalyser(entityContext);
        analyser.analyse(request);
        printAnalysis(analyser);

        DatabaseDataSet databaseDataSet = new DatabaseDataSet(entityContext);
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
