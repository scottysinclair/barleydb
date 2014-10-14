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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.server.jdbc.persister.*;
import scott.sort.server.jdbc.persister.exception.*;
import scott.sort.server.jdbc.resources.ConnectionResources;
import scott.sort.test.TestEntityContextServices.PersisterFactory;
import static org.junit.Assert.*;

import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.model.User;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.messaging.model.*;
import com.smartstream.messaging.query.*;

public class TestPersister extends TestBase {
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

    private AccessArea buildAccessAreas() {
        AccessArea root = entityContext.newModel(AccessArea.class);
        root.setName("root");
        AccessArea hsbc = entityContext.newModel(AccessArea.class);
        hsbc.setName("hsbc");
        hsbc.setParent(root);
        AccessArea jpmorgan = entityContext.newModel(AccessArea.class);
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
            XMLSyntaxModel syntaxModel = buildSyntax();
            print("", syntaxModel);
            PersistRequest request = new PersistRequest();
            request.save(syntaxModel);

            entityContext.persist(request);

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

        entityContext.persist(request);

        System.out.println("-------------- PRINTING NODE CONTEXT AFTER PERSIST  ------------------");
        printentityContext(entityContext);

        entityContext.clear();

        System.out.println("-------------- PRINTING RESULT OF PERIST ------------------");
        print("", root);

        System.out.println("-------------- QUERYING SAVED DATA  ------------------");
        QAccessArea qaa = new QAccessArea();
        qaa.joinToChildren().joinToChildren();
        qaa.where(qaa.name().equal("root"));

        List<AccessArea> result = entityContext.performQuery(qaa).getList();
        assertEquals(1, result.size());
        print("", result.get(0));
    }

    @Test
    public void testUpdateSyntax() throws Exception {
        /*
         * insert a new full model
         */
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model, for testing purposes, not actually necessary for updating
        */
        QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
        qsyntax.joinToUser();
        QXMLSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));

        entityContext.clear();
        XMLSyntaxModel syntax = entityContext.performQuery(qsyntax).getList().get(0);
        System.out.println(entityContext.printXml());
        print("", syntax);
        System.out.println(entityContext.printXml());
        assertNotNull(((ProxyController) syntax).getEntity().getOptimisticLock().getValue());

        /*
        * modify the syntax in various ways
        */
        System.out.println("-------------- Updating syntax name and mapping and subsyntax name and mapping ------------------");
        syntax.setName(syntax.getName() + " - updated");
        syntax.getMappings().get(0).setXpath("/updated-mapping");
        XMLSyntaxModel subSyntax = syntax.getMappings().get(1).getSubSyntaxModel();
        subSyntax.setName(subSyntax.getName() + " - updated");
        subSyntax.getMappings().get(0).setXpath("updated-submapping");

        System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
        entityContext.persist(new PersistRequest().save(syntax));

        System.out.println("-------------- RELOADING FROM SCRATCH TO OUTPUT THE REAL DATABASE DATA ------------------");
        entityContext.clear();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax - updated"));
        syntax = entityContext.performQuery(qsyntax).getList().get(0);
        print("", syntax);
    }

    @Test
    public void testEntityModifiedByAnotherUserDetected() throws Exception {
        /*
         * insert a new full model
         */
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        System.out.println("------------------- AFTER FIRST PERSIST\n" + entityContext.printXml() + "\n");

        /*
        * reload the full model for fun, not necessary for updating
        * We use another node context to get and update the syntax, simulating a concurrent user
        */
        QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
        qsyntax.joinToUser();
        QXMLSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));

        System.out.println("-------------- OTHER USER SAVING SYNTAX ------------------");
        EntityContext otherUser = new EntityContext(env, namespace);
        XMLSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        print("", otherSyntax);

        otherSyntax.getMappings().get(1).getSubSyntaxModel().getMappings().get(0).setXpath("/updated");
        //we save a mapping in the subsyntax
        //this will cause OL violation because subsyntax owned-by mapping owned-by syntax

        otherUser.persist(new PersistRequest().save(otherSyntax.getMappings().get(1).getSubSyntaxModel()));

        /*
         * Now saving the top level syntax model will fail because the subsyntax has been modified by another user
         */
        //perform a change that we want to save
        syntaxModel.getMappings().get(0).setXpath("/updated");
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            entityContext.persist(new PersistRequest().save(syntaxModel));
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
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model for fun, not necessary for updating
        */
        QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
        qsyntax.joinToUser();
        QXMLSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));

        /*
         * We use another node context to get and update the structure, simulating a concurrent user modification
         */
        EntityContext otherUser = new EntityContext(env, namespace);
        XMLSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        otherSyntax.getStructure().setName("updated-structure-name");
        otherUser.persist(new PersistRequest().save(otherSyntax.getStructure()));

        /*
        * modify the syntax in various ways
        */
        XMLSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntaxModel();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            entityContext.persist(new PersistRequest().save(syntaxModel));
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
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        /*
        * reload the full model for fun, not necessary for updating
        */
        QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
        qsyntax.joinToUser();
        QXMLSyntaxModel qsubSyntax = qsyntax.joinToMappings().joinToSubSyntax();
        qsubSyntax.joinToUser();
        qsubSyntax.joinToMappings();
        qsyntax.joinToStructure();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));

        /*
         * We use another node context to get and update the structure, simulating a concurrent user modification
         */
        EntityContext otherUser = new EntityContext(env, namespace);
        XMLSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
        otherUser.persist(new PersistRequest().delete(otherSyntax));

        /*
        * modify the syntax in various ways
        */
        XMLSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntaxModel();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            entityContext.persist(new PersistRequest().save(syntaxModel));
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
        if (!ConnectionResources.getMandatoryForPersist(entityContext).getDatabase().supportsBatchUpdateCounts()) {
            /**
             * If batch update counts are not supported then upfront optimistic locking is used and this test case is not relevant.
             */
            return;
        }

        final Persister persisterToTriggerConcurrentModifcation = new Persister(env, namespace, entityContextServices) {
            @Override
            protected void preJdbcWorkHook() {
                try {

                    QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
                    qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));

                    /*
                     * We use another node context to get and update the structure, simulating a concurrent user modification
                     */
                    EntityContext otherUser = new EntityContext(env, namespace);
                    otherUser.setAutocommit(false);
                    XMLSyntaxModel otherSyntax = otherUser.performQuery(qsyntax).getList().get(0);
                    entityContextServices.setPersisterFactory(null);
                    otherUser.persist( new PersistRequest().delete(otherSyntax) );
                }
                catch (Exception x) {}
            }
        };

        /*
         * insert a new full model
         */
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        entityContextServices.setPersisterFactory(new PersisterFactory() {
            @Override
            public Persister newPersister(Environment env, String namespace) {
                return persisterToTriggerConcurrentModifcation;
            }
        });

        /*
        * modify the syntax in various ways
        */
        XMLSyntaxModel subSyntax = syntaxModel.getMappings().get(1).getSubSyntaxModel();
        subSyntax.getMappings().get(0).setXpath("/updated-submapping");

        /*
         * Saving the syntax will now fail
         */
        try {
            System.out.println("-------------- CALLING PERSIST TO UPDATE CHANGES ------------------");
            entityContext.persist(new PersistRequest().save(syntaxModel));
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
        if (!ConnectionResources.getMandatoryForPersist(entityContext).getDatabase().supportsBatchUpdateCounts()) {
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
                    QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
                    qsyntax.joinToStructure();
                    qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));
                    EntityContext otherUser = new EntityContext(env, namespace);
                    otherUser.setAutocommit(false);
                    XMLSyntaxModel otherSyntaxCopy = otherUser.performQuery(qsyntax).getList().get(0);
                    otherSyntaxCopy.setName("Scott's Syntax updated-hook");

                    entityContextServices.setPersisterFactory(null);
                    otherUser.persist( new PersistRequest().save(otherSyntaxCopy) );
                }
                catch (Exception x) {}
            }
        };

        /*
         * insert a new full model
         */
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        entityContextServices.setPersisterFactory(new PersisterFactory() {
            @Override
            public Persister newPersister(Environment env, String namespace) {
                return persisterToTriggerConcurrentModifcation;
            }
        });

        syntaxModel.setName("Scott's Syntax updated");
        try {
            entityContext.persist(new PersistRequest().save(syntaxModel));
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
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        Long syntaxOl = getOptimisticLock(syntaxModel);
        Long subSyntaxOl = getOptimisticLock(syntaxModel.getMappings().get(1).getSubSyntaxModel());
        Assert.assertEquals(syntaxOl, subSyntaxOl);

        //save the sub-syntax with an updated mapping
        //we expect the sub-syntax ol to be touched but the syntax ol not.
        syntaxModel.getMappings().get(1).getSubSyntaxModel().getMappings().get(0).setXpath("/updated");
        entityContext.persist(new PersistRequest().save(syntaxModel.getMappings().get(1).getSubSyntaxModel()));

        /*
         * reload the syntax from the db
         */
        QXMLSyntaxModel qsyntax = new QXMLSyntaxModel();
        qsyntax.where(qsyntax.syntaxName().equal("Scott's Syntax"));
        XMLSyntaxModel updatedSyntaxModel = entityContext.performQuery(qsyntax).getList().get(0);

        //the optimistic lock of the original syntax is the same as before
        Long updatedSyntaxOl = getOptimisticLock(updatedSyntaxModel);
        Assert.assertEquals(syntaxOl, updatedSyntaxOl);

        Long updatedSubSyntaxOl = getOptimisticLock(updatedSyntaxModel.getMappings().get(1).getSubSyntaxModel());
        //the optimistic lock of the subsyntax is newer than before
        Assert.assertTrue(syntaxOl < updatedSubSyntaxOl);
    }

    @Test
    public void testSavingAnUnmodifiedSyntaxDoesNothing() throws Exception {
        /*
         * insert a new full model
         */
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        Long expectedOptimisticLock = getOptimisticLock(syntaxModel);

        entityContext.persist(new PersistRequest().save(syntaxModel));

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
        XMLSyntaxModel syntaxModel = buildSyntax();
        entityContext.persist(new PersistRequest().save(syntaxModel));

        /*
         * Then delete it.
         */
        entityContext.persist(new PersistRequest().delete(syntaxModel));

        /*
         * verify that the syntax was removed
         */
        assertTrue(entityContext.performQuery(new QXMLSyntaxModel()).getList().isEmpty());
        assertEquals(2, entityContext.size()); //only the user and the structure remain
    }

    @Test
    public void testSaveTemplateWithContentAndDatatypes() throws Exception {
        try {
            Template template = entityContext.newModel(Template.class);
            template.setName("test-template");

            TemplateContent content = entityContext.newModel(TemplateContent.class);
            content.setName("test-template-content-1");
            content.setTemplate(template);
            template.getContents().add(content);

            content = entityContext.newModel(TemplateContent.class);
            content.setName("test-template-content-2");
            //todo: setting is required in both directions (set + add)
            content.setTemplate(template);
            template.getContents().add(content);

            Datatype datatype = entityContext.newModel(Datatype.class);
            datatype.setName("test-datatype-1");
            //todo: setting not required in both directions (set + add)
            template.getDatatypes().add(datatype);

            datatype = entityContext.newModel(Datatype.class);
            datatype.setName("test-datatype-2");
            //todo: setting not required in both directions (set + add)
            template.getDatatypes().add(datatype);

            entityContext.persist(new PersistRequest().save(template));

            /*
             * For fun now a noop persist
             */
            System.out.println("===================  NOOP PERSIST =================");
            entityContext.persist(new PersistRequest()
                    .save(template)
                    .save(template.getDatatypes().get(0))
                    .save(template.getDatatypes().get(1)));
            System.out.println("===================  NOOP PERSIST =================");

            QTemplate qtemplate = new QTemplate();
            qtemplate.joinToContent();
            qtemplate.joinToDatatype();
            qtemplate.where(qtemplate.name().equal("test-template"));
            template = entityContext.performQuery(qtemplate).getSingleResult();
            print("", template);
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }
    }

    @Test
    public void testDeleteTemplateWithContent() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        entityContext.clear();

        System.out.println("===================  LOAD BEFORE DELETE  =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = entityContext.performQuery(qtemplate).getSingleResult();

        System.out.println("===================  DELETE  =================");
        entityContext.persist(new PersistRequest()
                .delete(template));

    }

    @Test
    public void testDeleteTemplateWithContentAndDatatypes() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        entityContext.clear();

        System.out.println("===================  LOAD BEFORE DELETE  =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = entityContext.performQuery(qtemplate).getSingleResult();

        System.out.println("===================  DELETE  =================");
        entityContext.persist(new PersistRequest()
                .delete(template)
                .delete(template.getDatatypes().get(0))
                .delete(template.getDatatypes().get(1))
                );

    }

    @Test
    public void testSaveTemplateFailsWhenDatatypeOutOfDate() throws Exception {
        testSaveTemplateWithContentAndDatatypes();
        entityContext.clear();

        System.out.println("===================  LOAD DATA FOR USER 1 =================");
        QTemplate qtemplate = new QTemplate();
        qtemplate.joinToDatatype();
        qtemplate.where(qtemplate.name().equal("test-template"));
        Template template = entityContext.performQuery(qtemplate).getSingleResult();

        System.out.println("===================  LOAD DATA FOR USER 2 =================");
        EntityContext ctx2 = new EntityContext(env, entityContext.getNamespace());
        Template template2 = ctx2.performQuery(qtemplate).getSingleResult();
        template2.getDatatypes().get(0).setName("updated-name");
        ctx2.persist(new PersistRequest().save(template2.getDatatypes().get(0)));

        System.out.println("===================  SAVE WHICH FAILS  =================");
        try {
            template.setName(template.getName() + "-updated");
            entityContext.persist(new PersistRequest().save(template));
        } catch (OptimisticLockMismatchException x) {
            assertEquals(template.getDatatypes().get(0).getId(), x.getEntity().getKey().getValue());
        }

    }

}
