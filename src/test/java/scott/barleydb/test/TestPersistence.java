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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.example.acl.model.AccessArea;
import org.example.acl.model.User;
import org.example.acl.query.QAccessArea;
import org.example.etl.context.MiEntityContext;
import org.example.etl.model.BusinessType;
import org.example.etl.model.RawData;
import org.example.etl.model.Template;
import org.example.etl.model.TemplateContent;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlStructure;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QRawData;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QXmlSyntaxModel;
import org.example.etl.model.SyntaxType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextHelper;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.persist.EntityMissingException;
import scott.barleydb.api.exception.execution.persist.OptimisticLockMismatchException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.server.jdbc.persist.Persister;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.test.TestEntityContextServices.PersisterFactory;

@RunWith(Parameterized.class)
public class TestPersistence extends TestRemoteClientBase {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new EntityContextGetter(false) },
                {new EntityContextGetter(true) }
           });
    }


    private EntityContextGetter getter;
    private EntityContext theEntityContext;

    public TestPersistence(EntityContextGetter getter) {
        this.getter = getter;
    }

//    public TestPersistence() {
//        this.getter = new EntityContextGetter(false);;
//    }

    @Override
    public void setup() throws Exception {
        super.setup();
        this.theEntityContext = getter.get(this);
    }

    /**
     * Builds a syntax creating 9 entities
     * 2 syntaxes
     * 5 mappings
     * 1 user
     * 1 structure
     * @return
     */
    public XmlSyntaxModel buildSyntax() {
        return buildSyntax(theEntityContext);
    }

    public static XmlSyntaxModel buildSyntax(EntityContext theEntityContext) {

        AccessArea root = theEntityContext.newModel(AccessArea.class);
        root.setName("root");

        XmlSyntaxModel syntaxModel = theEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);
        syntaxModel.setAccessArea(root);
        syntaxModel.setUuid("");


        User user = theEntityContext.newModel(User.class);
        user.setName("Jimmy");
        user.setAccessArea(root);
        user.setUuid("");

        syntaxModel.setUser(user);

        XmlStructure structure = theEntityContext.newModel(XmlStructure.class);
        structure.setName("scott's structure");
        structure.setAccessArea(root);
        structure.setUuid("");
        syntaxModel.setStructure(structure);

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

        //create the sub syntax
        XmlSyntaxModel subSyntaxModel = theEntityContext.newModel(XmlSyntaxModel.class);
        subSyntaxModel.setName("SubSyntaxModel - ooooh");
        subSyntaxModel.setAccessArea(root);
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);
        subSyntaxModel.setUuid("");

        mapping.setSubSyntax(subSyntaxModel); //set the subsyntax on the mapping

        //add another mapping to the root level syntax
        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(syntaxModel);
        mapping.setXpath("/root3");
        mapping.setTargetFieldName("target3");
        syntaxModel.getMappings().add(mapping);

        //do the sub-syntax mappings
        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel);
        mapping.setXpath("sub1");
        mapping.setTargetFieldName("subtarget1");
        subSyntaxModel.getMappings().add(mapping);

        mapping = theEntityContext.newModel(XmlMapping.class);
        mapping.setSyntax(subSyntaxModel);
        mapping.setXpath("sub2");
        mapping.setTargetFieldName("subtarget2");
        subSyntaxModel.getMappings().add(mapping);
        return syntaxModel;
    }

    public RawData buildRawData(String dataString) throws UnsupportedEncodingException {
        RawData rd = theEntityContext.newModel(RawData.class);
        rd.setCharacterEncoding("UTF-8");
        rd.setData(dataString.getBytes("UTF-8"));
        return rd;
    }

    private AccessArea buildAccessAreas() {
        AccessArea root = theEntityContext.newModel(AccessArea.class);
        root.setName("root");
        AccessArea hsbc = theEntityContext.newModel(AccessArea.class);
        hsbc.setName("hsbc");
        hsbc.setParent(root);
        AccessArea jpmorgan = theEntityContext.newModel(AccessArea.class);
        jpmorgan.setName("jpmorgan");
        jpmorgan.setParent(root);
        root.getChildren().add(hsbc);
        root.getChildren().add(jpmorgan);
        return root;
    }

    @Test
    public void testPersistNewXMLSyntax() throws Exception {
        try {
            System.out.println("STARTING TEST testPersistNewXMLSyntax");
            XmlSyntaxModel syntaxModel = buildSyntax();
            print("", syntaxModel);
            PersistRequest request = new PersistRequest();
            request.save(syntaxModel);

            theEntityContext.persist(request);

            System.out.println("-------------- PRINTING RESULT OF PERIST ------------------");
            print("", syntaxModel);
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }

    @Test
    public void testPersistNewAccessAreaHierarchy() throws Exception {
        System.out.println("STARTING TEST testPersistNewAccessAreaHierarchy");
        AccessArea root = buildAccessAreas();

        PersistRequest request = new PersistRequest();
        request.save(root);

        theEntityContext.persist(request);

        System.out.println("-------------- PRINTING NODE CONTEXT AFTER PERSIST  ------------------");
        printEntityContext(theEntityContext);

        theEntityContext.clear();

        System.out.println("-------------- PRINTING RESULT OF PERIST ------------------");
        print("", root);

        System.out.println("-------------- QUERYING SAVED DATA  ------------------");
        QAccessArea qaa = new QAccessArea();
        qaa.joinToChildren().joinToChildren();
        qaa.where(qaa.name().equal("root"));

        List<AccessArea> result = theEntityContext.performQuery(qaa).getList();
        assertEquals(1, result.size());
        print("", result.get(0));
    }

    @Test
    public void testUpdateSyntax() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model, for testing purposes, not actually necessary for updating
        */
        QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
        qsyntax.joinToUser();
        QXmlSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));

        theEntityContext.clear();
        XmlSyntaxModel syntax = theEntityContext.performQuery(qsyntax).getList().get(0);
        System.out.println(theEntityContext.printXml());
        print("", syntax);
        System.out.println(theEntityContext.printXml());
        assertNotNull(((ProxyController) syntax).getEntity().getOptimisticLock().getValue());

        /*
        * modify the syntax in various ways
        */
        System.out.println("-------------- Updating syntax name and mapping and subsyntax name and mapping ------------------");
        syntax.setName(syntax.getName() + " - updated");
        syntax.getMappings().get(0).setXpath("/updated-mapping");
        XmlSyntaxModel subSyntax = syntax.getMappings().get(1).getSubSyntax();
        subSyntax.setName(subSyntax.getName() + " - updated");
        subSyntax.getMappings().get(0).setXpath("updated-submapping");

        System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
        theEntityContext.persist(new PersistRequest().save(syntax));

        System.out.println("-------------- RELOADING FROM SCRATCH TO OUTPUT THE REAL DATABASE DATA ------------------");
        theEntityContext.clear();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel - updated"));
        syntax = theEntityContext.performQuery(qsyntax).getList().get(0);
        print("", syntax);
    }

    @Test
    public void testEntityModifiedByAnotherUserDetected() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        System.out.println("------------------- AFTER FIRST PERSIST\n" + theEntityContext.printXml() + "\n");

        /*
        * reload the full model for fun, not necessary for updating
        * We use another node context to get and update the syntax, simulating a concurrent user
        */
        QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
        qsyntax.joinToUser();
        QXmlSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));

        System.out.println("-------------- OTHER USER SAVING SYNTAX ------------------");
        EntityContext otherUser = new MiEntityContext(env);
        XmlSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        print("", otherSyntax);

        otherSyntax.getMappings().get(1).getSubSyntax().getMappings().get(0).setXpath("/updated");
        //we save a mapping in the subsyntax
        //this will cause OL violation because subsyntax owned-by mapping owned-by syntax

        otherUser.persist(new PersistRequest().save(otherSyntax.getMappings().get(1).getSubSyntax()));

        /*
         * Now saving the top level syntax model will fail because the subsyntax has been modified by another user
         */
        //perform a change that we want to save
        syntaxModel.getMappings().get(0).setXpath("/updated");
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            theEntityContext.persist(new PersistRequest().save(syntaxModel));
            fail("expected OptimisticLockMismatchException");
        } catch (OptimisticLockMismatchException x) {
            //expected
            System.out.println("Correctly detected optimistic lock violation:" + x.getMessage());
        }
    }

    /**
     * Tests that optimistic locking respects the depends on relationship
     * @throws Exception
     */
    @Test
    public void testStructureModifiedByAnotherUserDetectedWhenSavingSyntax() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model for fun, not necessary for updating
        */
        QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
        qsyntax.joinToUser();
        QXmlSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));

        /*
         * We use another node context to get and update the structure, simulating a concurrent user modification
         */
        EntityContext otherUser = new MiEntityContext(env);
        XmlSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        otherSyntax.getStructure().setName("updated-structure-name");
        otherUser.persist(new PersistRequest().save(otherSyntax.getStructure()));

        /*
        * modify the syntax in various ways
        */
        XmlSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntax();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            theEntityContext.persist(new PersistRequest().save(syntaxModel));
            fail("expected OptimisticLockMismatchException");
        } catch (OptimisticLockMismatchException x) {
            //expected
        }
    }

    /**
     * Tests the exception when you try and update something that was deleted
     * @throws Exception
     */
    @Test
    public void testSavingSyntaxADeletedSyntaxFailsCorrectly() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model for fun, not necessary for updating
        */
        QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
        qsyntax.joinToUser();
        QXmlSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));

        /*
         * We use another node context to get and update the structure, simulating a concurrent user modification
         */
        EntityContext otherUser = new MiEntityContext(env);
        XmlSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        otherUser.persist(new PersistRequest().delete(otherSyntax));

        /*
        * modify the syntax in various ways
        */
        XmlSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntax();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            theEntityContext.persist(new PersistRequest().save(syntaxModel));
            fail("expected EntityMissingException");
        } catch (EntityMissingException x) {
            Entity entityToSave = ((ProxyController) syntaxModel).getEntity();
            Assert.assertSame(entityToSave.getEntityType(), x.getEntityType());
            Assert.assertSame(entityToSave.getKey().getValue(), syntaxModel.getId());
        }
    }

    /**
     * Tests the exception when you try and update something that was deleted during your persist operation.
     * The failure is detected by checking the batch update counts and seeing that there was a noop.
     * @throws Exception
     */
    @Test
    public void testSyntaxDeletedJustBeforeJdbcUpdateOperation() throws Exception {
        /*
         * this check is for test and is a "server side" check on the entity context of the server.
         */
        if (!ConnectionResources.getMandatoryForPersist(serverEntityContext).getDatabase().supportsBatchUpdateCounts()) {
            /**
             * If batch update counts are not supported then upfront optimistic locking is used and this test case is not relevant.
             */
            return;
        }

        final Persister persisterToTriggerConcurrentModifcation = new Persister(env, namespace, entityContextServices) {
            @Override
            protected void preJdbcWorkHook() {
                try {
                    QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
                    qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));

                    /*
                     * We use another node context to get and update the structure, simulating a concurrent user modification
                     */
                    EntityContext otherUser = new MiEntityContext(env);
                    otherUser.setAutocommit(false);
                    XmlSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
                    entityContextServices.setPersisterFactory(null);
                    otherUser.persist( new PersistRequest().delete(otherSyntax) );
                    otherUser.commit();
                }
                catch (Exception x) {}
            }
        };

        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        entityContextServices.setPersisterFactory(new PersisterFactory() {
            @Override
            public Persister newPersister(Environment env, String namespace) {
                return persisterToTriggerConcurrentModifcation;
            }
        });

        /*
        * modify the syntax in various ways
        */
        XmlSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntax();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            theEntityContext.persist(new PersistRequest().save(syntaxModel));
            fail("expected EntityMissingException");
        } catch (EntityMissingException x) {
            Entity entityToSave = ((ProxyController) syntaxModel).getEntity();
            Assert.assertSame(entityToSave.getEntityType(), x.getEntityType());
            Assert.assertSame(entityToSave.getKey().getValue(), syntaxModel.getId());
        }
        catch(Exception x) {
            x.printStackTrace(System.err);
            throw x;
        }
    }

    /**
     * Performs a concurrent modification during a persist call.
     * The concurrent modification causes our persist call to fail (due to checking the batch update counts)
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentModificationCausesActualUpdateCallToFail() throws Exception {
        /*
         * this check is for test and is a "server side" check on the entity context of the server.
         */
        if (!ConnectionResources.getMandatoryForPersist(serverEntityContext).getDatabase().supportsBatchUpdateCounts()) {
            /**
             * If batch update counts are not supported then upfront optimistic locking is used and this test case is not relevant.
             */
            return;
        }

        final Persister persisterToTriggerConcurrentModifcation = new Persister(env, namespace, entityContextServices) {
            @Override
            protected void preJdbcWorkHook() {
                try {
                    /*
                     * We use another node context to get and update the structure, simulating a concurrent user modification
                     */
                    QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
                    qsyntax.joinToStructure();
                    qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));
                    EntityContext otherUser = new MiEntityContext(env);
                    otherUser.setAutocommit(false);
                    XmlSyntaxModel otherSyntaxCopy = otherUser.performQuery(qsyntax).getList().get(0);
                    otherSyntaxCopy.setName("Scott's SyntaxModel updated-hook");

                    entityContextServices.setPersisterFactory(null);
                    otherUser.persist( new PersistRequest().save(otherSyntaxCopy) );
                    otherUser.commit();
                    otherUser.setAutocommit(true);
                }
                catch (Exception x) {}
            }
        };

        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        entityContextServices.setPersisterFactory(new PersisterFactory() {
            @Override
            public Persister newPersister(Environment env, String namespace) {
                return persisterToTriggerConcurrentModifcation;
            }
        });

        syntaxModel.setName("Scott's SyntaxModel updated");
        try {
            theEntityContext.persist(new PersistRequest().save(syntaxModel));
            Assert.fail("Expected OptimisticLockMismatchException");
        } catch (OptimisticLockMismatchException x) {
            Entity entityWantedSave = ((ProxyController) syntaxModel).getEntity();
            Entity entityFromDb = x.getDatabaseEntity();
            Assert.assertSame(entityWantedSave, x.getEntity()); //the fail was from the syntax we wanted to update
            Assert.assertEquals(entityWantedSave.getEntityType(), entityFromDb.getEntityType());
            Assert.assertEquals(entityWantedSave.getKey().getValue(), entityFromDb.getKey().getValue());
            //assert that the database entity has a newer optimistic lock timestamp than the entity we wanted to save
            Assert.assertTrue(((Comparable<Object>) entityFromDb.getOptimisticLock().getValue()).compareTo(
                    (Comparable<Object>) entityWantedSave.getOptimisticLock().getValue()) > 0);
        }
    }

    @Test
    public void testSubSyntaxOptimisticLockIsUpdatedWhenUpdatingItsMapping() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        Long syntaxOl = getOptimisticLock(syntaxModel);
        Long subSyntaxOl = getOptimisticLock(syntaxModel.getMappings().get(1).getSubSyntax());
        Assert.assertEquals(syntaxOl, subSyntaxOl);

        //save the sub-syntax with an updated mapping
        //we expect the sub-syntax ol to be touched but the syntax ol not.
        syntaxModel.getMappings().get(1).getSubSyntax().getMappings().get(0).setXpath("/updated");
        theEntityContext.persist(new PersistRequest().save(syntaxModel.getMappings().get(1).getSubSyntax()));

        /*
         * reload the syntax from the db
         */
        QXmlSyntaxModel qsyntax = new QXmlSyntaxModel();
        qsyntax.where(qsyntax.name().equal("Scott's SyntaxModel"));
        XmlSyntaxModel updatedSyntaxModel = theEntityContext.performQuery(qsyntax).getList().get(0);

        //the optimistic lock of the original syntax is the same as before
        Long updatedSyntaxOl = getOptimisticLock(updatedSyntaxModel);
        Assert.assertEquals(syntaxOl, updatedSyntaxOl);

        Long updatedSubSyntaxOl = getOptimisticLock(updatedSyntaxModel.getMappings().get(1).getSubSyntax());
        //the optimistic lock of the subsyntax is newer than before
        Assert.assertTrue(syntaxOl < updatedSubSyntaxOl);
    }

    @Test
    public void testSavingAnUnmodifiedSyntaxDoesNothing() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        Long expectedOptimisticLock = getOptimisticLock(syntaxModel);

        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        Long actualOptimisticLock = getOptimisticLock(syntaxModel);
        Assert.assertEquals(expectedOptimisticLock, actualOptimisticLock);
    }

    private static Long getOptimisticLock(Object object) {
        return (Long) ((ProxyController) object).getEntity().getOptimisticLock().getValue();
    }

    @Test
    public void testDeleteSyntax() throws Exception {
        /*
         * insert a new full model
         */
        XmlSyntaxModel syntaxModel = buildSyntax();
        theEntityContext.persist(new PersistRequest().save(syntaxModel));

        /*
         * Then delete it.
         */
        theEntityContext.persist(new PersistRequest().delete(syntaxModel));

        printEntityContext(theEntityContext);

        /*
         * verify that the syntax was removed
         */
        assertTrue(theEntityContext.performQuery(new QXmlSyntaxModel()).getList().isEmpty());
        assertEquals(10, theEntityContext.size());
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(AccessArea.class) ) );
        assertEquals(2, EntityContextHelper.countNew( theEntityContext.getEntitiesByType(XmlSyntaxModel.class) ) );
        assertEquals(5, EntityContextHelper.countNew( theEntityContext.getEntitiesByType(XmlMapping.class) ) );
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(XmlStructure.class) ) );
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(User.class) ) );
    }

    @Test
    public void testSaveTemplateWithContentAndDatatypes() throws Exception {
        try {
            AccessArea root = theEntityContext.newModel(AccessArea.class);
            root.setName("root");
            Template template = theEntityContext.newModel(Template.class);
            template.setName("test-template");
            template.setAccessArea(root);
            template.setUuid("");

            TemplateContent content = theEntityContext.newModel(TemplateContent.class);
            content.setName("test-template-content-1");
            content.setTemplate(template);
            template.getContents().add(content);

            content = theEntityContext.newModel(TemplateContent.class);
            content.setName("test-template-content-2");
            //todo: setting is required in both directions (set + add)
            content.setTemplate(template);
            template.getContents().add(content);

            BusinessType datatype = theEntityContext.newModel(BusinessType.class);
            datatype.setName("test-datatype-1");
            datatype.setAccessArea(root);
            datatype.setUuid("");

            //todo: setting not required in both directions (set + add)
            template.getBusinessTypes().add(datatype);

            datatype = theEntityContext.newModel(BusinessType.class);
            datatype.setName("test-datatype-2");
            datatype.setAccessArea(root);
            datatype.setUuid("");
            //todo: setting not required in both directions (set + add)
            template.getBusinessTypes().add(datatype);

            theEntityContext.persist(new PersistRequest().save(template));

            /*
             * For fun now a noop persist
             */
            System.out.println("===================  NOOP PERSIST =================");
            theEntityContext.persist(new PersistRequest()
                    .save(template)
                    .save(template.getBusinessTypes().get(0))
                    .save(template.getBusinessTypes().get(1)));
            System.out.println("===================  NOOP PERSIST =================");

            QTemplate qtemplate = new QTemplate();
            qtemplate.joinToContents();
            qtemplate.joinToBusinessType();
            qtemplate.where(qtemplate.name().equal("test-template"));
            template = theEntityContext.performQuery(qtemplate).getSingleResult();
            print("", template);
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }

    @Test
    public void testDeleteTemplateWithContent() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        theEntityContext.clear();

        System.out.println("===================  LOAD BEFORE DELETE  =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = theEntityContext.performQuery(qtemplate).getSingleResult();

        /*
         * only the template is in the context
         */
        assertEquals(2, theEntityContext.size());
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(Template.class) ) );
        assertEquals(1, EntityContextHelper.countNotLoaded( theEntityContext.getEntitiesByType(AccessArea.class) ) );

        System.out.println("===================  DELETE  =================");
        theEntityContext.persist(new PersistRequest()
                .delete(template));

    }

    @Test
    public void testDeleteTemplateWithContentAndDatatypes() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        theEntityContext.clear();

        System.out.println("===================  LOAD BEFORE DELETE  =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = theEntityContext.performQuery(qtemplate).getSingleResult();

        System.out.println("===================  DELETE  =================");
        theEntityContext.persist(new PersistRequest()
                .delete(template)
                .delete(template.getBusinessTypes().get(0))
                .delete(template.getBusinessTypes().get(1))
                );

    }

    @Test
    public void testSaveTemplateFailsWhenDatatypeOutOfDate() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        theEntityContext.clear();

        System.out.println("===================  LOAD DATA FOR USER 1 =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.joinToBusinessType();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = theEntityContext.performQuery(qtemplate).getSingleResult();

        System.out.println("===================  LOAD DATA FOR USER 2 =================");
        EntityContext ctx2 = new MiEntityContext(env);
        Template template2 = ctx2.performQuery(qtemplate).getSingleResult();
        template2.getBusinessTypes().get(0).setName("updated-name");
        ctx2.persist(new PersistRequest().save(template2.getBusinessTypes().get(0)));

        System.out.println("===================  SAVE WHICH FAILS  =================");
        try {
            template.setName(template.getName() + "-updated");
            theEntityContext.persist(new PersistRequest().save(template));
        } catch (OptimisticLockMismatchException x) {
            assertEquals(template.getBusinessTypes().get(0).getId(), x.getEntity().getKey().getValue());
        }
    }

    @Test
    public void testSaveRawData() throws Exception {
        RawData rd = buildRawData("rawdata");
        theEntityContext.persist(new PersistRequest().save(rd));

        for (RawData r: theEntityContext.performQuery(new QRawData()).getList()) {
            System.out.println(r.getId());
            System.out.println(r.getData().length);
            System.out.println(r.getCharacterEncoding());
        }
   }

    @Test
    public void testSaveSyntaxWithMappingWhichIsPerhapsInTheDatabaseAndIsNot() throws SortException {
        //first create a syntax with 2 mappings
        XmlSyntaxModel syntax = theEntityContext.newModel(XmlSyntaxModel.class);
        syntax.setUser( theEntityContext.newModel(User.class) );
        syntax.setAccessArea( theEntityContext.newModel(AccessArea.class) );
        syntax.setName("whatever");
        syntax.setUuid("");
        syntax.setSyntaxType(SyntaxType.ROOT);
        syntax.setStructure( theEntityContext.newModel(XmlStructure.class));

        syntax.getAccessArea().setName("root");
        syntax.getUser().setName("fred");
        syntax.getUser().setAccessArea(syntax.getAccessArea());
        syntax.getUser().setUuid("");
        syntax.getStructure().setName("struct");
        syntax.getStructure().setAccessArea(syntax.getAccessArea());
        syntax.getStructure().setUuid("");

        XmlMapping m1 = theEntityContext.newModel(XmlMapping.class);
        m1.setSyntax(syntax);
        m1.setXpath("/root");
        m1.setTargetFieldName("root");
        syntax.getMappings().add(m1);

        theEntityContext.persist(new PersistRequest().save(syntax));

        /*
         * now we do something 'stupid' we add an XmlMapping entity to the entity context with key 100
         * and we say we don't know if it already exists or not.
         *
         * We then add it to the syntax and persist the syntax, we expect it to work.
         * In the real world this would not be a good idea if the PK is framework generated, ie the
         * PK could be in use.
         */
        XmlMapping m2PerhapsInDb = theEntityContext.newOrNotLoadedModel(XmlMapping.class, 100L);
        m2PerhapsInDb.setSyntax(syntax);
        m2PerhapsInDb.setXpath("/root");
        m2PerhapsInDb.setTargetFieldName("root");
        syntax.getMappings().add(m2PerhapsInDb);

        assertEquals((Long)100L, m2PerhapsInDb.getId());
        assertTrue(m2PerhapsInDb.getEntity().isPerhapsInDatabase());

        theEntityContext.persist(new PersistRequest().save(syntax));
        assertFalse(m2PerhapsInDb.getEntity().isPerhapsInDatabase());
        assertTrue(m2PerhapsInDb.getEntity().isLoaded());
        assertEquals((Long)100L, m2PerhapsInDb.getId());
    }

    @Test
    public void testSaveSyntaxWithDeletedMappingWhichIsPerhapsInTheDatabaseAndIsNot() throws SortException {
        //first create a syntax with 2 mappings
        XmlSyntaxModel syntax = theEntityContext.newModel(XmlSyntaxModel.class);
        syntax.setUser( theEntityContext.newModel(User.class) );
        syntax.setAccessArea( theEntityContext.newModel(AccessArea.class) );
        syntax.setName("whatever");
        syntax.setUuid("");
        syntax.setSyntaxType(SyntaxType.ROOT);
        syntax.setStructure( theEntityContext.newModel(XmlStructure.class));

        syntax.getAccessArea().setName("root");
        syntax.getUser().setName("fred");
        syntax.getUser().setAccessArea(syntax.getAccessArea());
        syntax.getUser().setUuid("");
        syntax.getStructure().setName("struct");
        syntax.getStructure().setAccessArea(syntax.getAccessArea());
        syntax.getStructure().setUuid("");

        XmlMapping m1 = theEntityContext.newModel(XmlMapping.class);
        m1.setSyntax(syntax);
        m1.setXpath("/root");
        m1.setTargetFieldName("root");
        syntax.getMappings().add(m1);

        theEntityContext.persist(new PersistRequest().save(syntax));

        /*
         * now we do something 'stupid' we add an XmlMapping entity to the entity context with key 100
         * and we say we don't know if it already exists or not.
         *
         * We then add it to the syntax and persist the syntax, we expect it to work.
         * In the real world this would not be a good idea if the PK is framework generated, ie the
         * PK could be in use.
         */
        XmlMapping m2PerhapsInDb = theEntityContext.newOrNotLoadedModel(XmlMapping.class, 100L);
        m2PerhapsInDb.setSyntax(syntax);
        m2PerhapsInDb.setXpath("/root");
        m2PerhapsInDb.setTargetFieldName("root");
        syntax.getMappings().add(m2PerhapsInDb);

        assertEquals((Long)100L, m2PerhapsInDb.getId());
        assertTrue(m2PerhapsInDb.getEntity().isPerhapsInDatabase());

        syntax.getMappings().remove(m2PerhapsInDb);

        /*
         * the following persist should be a noop.
         */
        theEntityContext.persist(new PersistRequest().save(syntax));
        /*
         * the persist attempt figured out that m2PerhapsInDb is new and so adjusted the EntityState
         * accordingly. (wow)
         */
        assertTrue(m2PerhapsInDb.getEntity().isNew());
        assertEquals((Long)100L, m2PerhapsInDb.getId());
    }


}
