package scott.barleydb.test;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.List;

import org.example.acl.model.AccessArea;
import org.example.acl.model.User;
import org.example.etl.model.SyntaxType;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlStructure;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QXmlSyntaxModel;
import org.junit.Assert;
import org.junit.Test;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.persist.PersistRequest;

public class TestBatchFetch extends TestRemoteClientBase {

    private EntityContextGetter getter;
    private EntityContext theEntityContext;

    public TestBatchFetch() {
        this.getter = new EntityContextGetter(false);
        this.autoCommitMode = false;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        this.theEntityContext = getter.get(this);
    }

    @Test
    public void testBatchFetch() throws SortServiceProviderException, SortPersistException, BarleyDBQueryException {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax(theEntityContext);
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        theEntityContext.clear();

        QXmlSyntaxModel query = new QXmlSyntaxModel();
        syntaxModel = theEntityContext.performQuery(query.where(query.id().equal(syntaxModel.getId()))).getSingleResult();

        theEntityContext.batchFetchDescendants(syntaxModel);

        System.out.println();
        System.out.println("--------- BATCH FETCH   BATCH FETCH  BATCH FETCH  ---------------------------------------------------------------------");
        System.out.println();

        List<XmlMapping> mappings = syntaxModel.getMappings();
        mappings.get(0);
        System.out.println();
        System.out.println("--------- BATCH FETCH   SUBSYNTAX SUBSYNTAX ---------------------------------------------------------------------");

        theEntityContext.switchToInternalMode();
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(1).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(2).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(3).getSubSyntax().getEntity().getEntityState());

        theEntityContext.switchToExternalMode();
        mappings.get(1).getSubSyntax().getName();

        theEntityContext.switchToInternalMode();
        Assert.assertEquals(EntityState.LOADED, mappings.get(1).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.LOADED, mappings.get(2).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.LOADED, mappings.get(3).getSubSyntax().getEntity().getEntityState());
        Assert.assertFalse(mappings.get(1).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertFalse(mappings.get(2).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertFalse(mappings.get(3).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());

        theEntityContext.switchToExternalMode();
        mappings.get(1).getSubSyntax().getMappings().size(); //fetch the subsyntax mappings
        theEntityContext.switchToInternalMode();
        Assert.assertTrue(mappings.get(1).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertTrue(mappings.get(2).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertTrue(mappings.get(3).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertEquals(2, syntaxModel.getMappings().get(1).getSubSyntax().getMappings().size());
        Assert.assertEquals(0, syntaxModel.getMappings().get(2).getSubSyntax().getMappings().size());
        Assert.assertEquals(0, syntaxModel.getMappings().get(2).getSubSyntax().getMappings().size());
    }

    @Test
    public void testNonBatchFetch() throws SortServiceProviderException, SortPersistException, BarleyDBQueryException {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax(theEntityContext);
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        theEntityContext.clear();

        QXmlSyntaxModel query = new QXmlSyntaxModel();
        syntaxModel = theEntityContext.performQuery(query.where(query.id().equal(syntaxModel.getId()))).getSingleResult();

        List<XmlMapping> mappings = syntaxModel.getMappings();
        mappings.get(0);

        theEntityContext.switchToInternalMode();
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(1).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(2).getSubSyntax().getEntity().getEntityState());

        theEntityContext.switchToExternalMode();
        mappings.get(1).getSubSyntax().getName();

        theEntityContext.switchToInternalMode();
        Assert.assertEquals(EntityState.LOADED, mappings.get(1).getSubSyntax().getEntity().getEntityState());
        Assert.assertEquals(EntityState.NOTLOADED, mappings.get(2).getSubSyntax().getEntity().getEntityState());
        Assert.assertFalse(mappings.get(1).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertFalse(mappings.get(2).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());

        theEntityContext.switchToExternalMode();
        mappings.get(2).getSubSyntax().getName(); //load the second subsyntax
        mappings.get(1).getSubSyntax().getMappings().size(); //fetch the subsyntax mappings
        theEntityContext.switchToInternalMode();
        Assert.assertTrue(mappings.get(1).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertFalse(mappings.get(2).getSubSyntax().getEntity().getChild("mappings", ToManyNode.class).isFetched());
        Assert.assertEquals(2, syntaxModel.getMappings().get(1).getSubSyntax().getMappings().size());
    }

    public static XmlSyntaxModel buildSyntax(EntityContext theEntityContext) {

        AccessArea root = theEntityContext.newModel(AccessArea.class);
        root.setName("root");

        XmlSyntaxModel syntaxModel = theEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);
        syntaxModel.setAccessArea(root);
        syntaxModel.setUuid("");


        User user1 = theEntityContext.newModel(User.class);
        user1.setName("Jimmy1");
        user1.setAccessArea(root);
        user1.setUuid("");

        syntaxModel.setUser(user1);

        XmlStructure structure1 = theEntityContext.newModel(XmlStructure.class);
        structure1.setName("scott's structure1");
        structure1.setAccessArea(root);
        structure1.setUuid("");
        syntaxModel.setStructure(structure1);

        XmlMapping mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root1");
        mapping.setTargetFieldName("target1");
        syntaxModel.getMappings().add(mapping);

        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root2");
        mapping.setTargetFieldName("target2");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax 1
        XmlStructure structure2 = theEntityContext.newModel(XmlStructure.class);
        structure2.setName("scott's structure2");
        structure2.setAccessArea(root);
        structure2.setUuid("");

        User user2 = theEntityContext.newModel(User.class);
        user2.setName("Jimmy2");
        user2.setAccessArea(root);
        user2.setUuid("");

        XmlSyntaxModel subSyntaxModel1 = theEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel1.setName("SubSyntaxModel - ooooh1");
        subSyntaxModel1.setAccessArea(root);
        subSyntaxModel1.setStructure(structure2);
        subSyntaxModel1.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel1.setUser(user2);
        subSyntaxModel1.setUuid("");

        mapping.setSubSyntax(subSyntaxModel1); //set the subsyntax on the mapping

        //add another mapping to the root level syntax
        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTargetFieldName("target3");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax 2
        XmlStructure structure3 = theEntityContext.newModel(XmlStructure.class);
        structure3.setName("scott's structure3");
        structure3.setAccessArea(root);
        structure3.setUuid("");

        User user3 = theEntityContext.newModel(User.class);
        user3.setName("Jimmy3");
        user3.setAccessArea(root);
        user3.setUuid("");

        XmlSyntaxModel subSyntaxModel2 = theEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel2.setName("SubSyntaxModel - ooooh2");
        subSyntaxModel2.setAccessArea(root);
        subSyntaxModel2.setStructure(structure3);
        subSyntaxModel2.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel2.setUser(user3);
        subSyntaxModel2.setUuid("");

        mapping.setSubSyntax(subSyntaxModel2); //set the subsyntax on the mapping

        //add another mapping to the root level syntax
        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root4");
        mapping.setTargetFieldName("target3");
        syntaxModel.getMappings().add(mapping);

        //create the sub syntax 2
        XmlStructure structure4 = theEntityContext.newModel(XmlStructure.class);
        structure4.setName("scott's structure4");
        structure4.setAccessArea(root);
        structure4.setUuid("");

        User user4 = theEntityContext.newModel(User.class);
        user4.setName("Jimmy4");
        user4.setAccessArea(root);
        user4.setUuid("");

        XmlSyntaxModel subSyntaxModel3 = theEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel3.setName("SubSyntaxModel - ooooh3");
        subSyntaxModel3.setAccessArea(root);
        subSyntaxModel3.setStructure(structure3);
        subSyntaxModel3.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel3.setUser(user4);
        subSyntaxModel3.setUuid("");

        mapping.setSubSyntax(subSyntaxModel3); //set the subsyntax on the mapping


        //do the sub-syntax mappings
        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel1);
        mapping.setXpath("sub1");
        mapping.setTargetFieldName("subtarget1");
        subSyntaxModel1.getMappings().add(mapping);

        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel1);
        mapping.setXpath("sub2");
        mapping.setTargetFieldName("subtarget2");
        subSyntaxModel1.getMappings().add(mapping);
        return syntaxModel;
    }

}
