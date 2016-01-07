package scott.barleydb.test;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
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

import static org.junit.Assert.assertEquals;

import org.example.acl.model.AccessArea;
import org.example.acl.model.User;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlStructure;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.types.SyntaxType;
import org.junit.Test;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.server.jdbc.persist.DatabaseDataSet;

public class TestDatabaseDataSet extends TestBase {

    @Test
    public void testDatabaseDataSet() throws Exception {
        AccessArea root = serverEntityContext.newModel(AccessArea.class);
        root.setName("root");
        XmlSyntaxModel syntaxModel = serverEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setAccessArea(root);
        syntaxModel.setUuid("");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);

        User user = serverEntityContext.newModel(User.class);
        user.setName("Jimmy");
        user.setAccessArea(root);
        user.setUuid("");

        syntaxModel.setUser(user);

        XmlStructure structure = serverEntityContext.newModel(XmlStructure.class);
        structure.setName("scott's structure");
        structure.setAccessArea(root);
        structure.setUuid("");
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
        subSyntaxModel.setAccessArea(root);
        subSyntaxModel.setUuid("");
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
            if (!entity.isFetchRequired()) {
                count++;
            }
        }
        return count;
    }
}
