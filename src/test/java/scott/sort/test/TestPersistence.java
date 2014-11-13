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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityContextHelper;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.exception.execution.persist.EntityMissingException;
import scott.sort.api.exception.execution.persist.OptimisticLockMismatchException;
import scott.sort.api.persist.PersistRequest;
import scott.sort.server.jdbc.persist.Persister;
import scott.sort.server.jdbc.resources.ConnectionResources;
import scott.sort.test.TestEntityContextServices.PersisterFactory;

import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.model.User;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mi.context.MiEntityContext;
import com.smartstream.mi.model.BusinessType;
import com.smartstream.mi.model.Template;
import com.smartstream.mi.model.TemplateContent;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.model.XmlStructure;
import com.smartstream.mi.model.XmlSyntaxModel;
import com.smartstream.mi.query.QTemplate;
import com.smartstream.mi.query.QXmlSyntaxModel;
import com.smartstream.mi.types.SyntaxType;

@SuppressWarnings("deprecation")
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

    protected void prepareData() {
        super.prepareData();
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/clean.sql"), false);
    }


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
//        root.setName("root");

        XmlSyntaxModel syntaxModel = theEntityContext.newModel(XmlSyntaxModel.class);
        syntaxModel.setName("Scott's SyntaxModel");
        syntaxModel.setSyntaxType(SyntaxType.ROOT);


        User user = theEntityContext.newModel(User.class);
        user.setName("Jimmy");

        syntaxModel.setUser(user);

        XmlStructure structure = theEntityContext.newModel(XmlStructure.class);
        structure.setName("scott's structure");
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
        subSyntaxModel.setStructure(structure);
        subSyntaxModel.setSyntaxType(SyntaxType.SUBSYNTAX);
        subSyntaxModel.setUser(user);

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
        assertEquals(9, theEntityContext.size());
        assertEquals(2, EntityContextHelper.countNotLoaded( theEntityContext.getEntitiesByType(XmlSyntaxModel.class) ) );
        assertEquals(5, EntityContextHelper.countNotLoaded( theEntityContext.getEntitiesByType(XmlMapping.class) ) );
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(XmlStructure.class) ) );
        assertEquals(1, EntityContextHelper.countLoaded( theEntityContext.getEntitiesByType(User.class) ) );
    }

    @Test
    public void testSaveTemplateWithContentAndDatatypes() throws Exception {
        try {
            Template template = theEntityContext.newModel(Template.class);
            template.setName("test-template");

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
            //todo: setting not required in both directions (set + add)
            template.getBusinessTypes().add(datatype);

            datatype = theEntityContext.newModel(BusinessType.class);
            datatype.setName("test-datatype-2");
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
        assertEquals(1, theEntityContext.size());

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

}
