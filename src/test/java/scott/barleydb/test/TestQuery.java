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

import org.example.acl.query.QUser;
import org.example.etl.model.*;
import org.example.etl.query.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.stream.DataStream;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.server.jdbc.query.QueryResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static scott.barleydb.api.query.JoinType.INNER;
import static scott.barleydb.api.query.JoinType.LEFT_OUTER;

/**
 * Tests various types of queries in a server environment and a remote client environment.
 *
 * Remote clients cannot set auto-commit to false which is not required for these tests.
 *
 * @author scott
 *
 */
@SuppressWarnings({ "deprecation", "unused" })
@RunWith(Parameterized.class)
public class TestQuery extends TestRemoteClientBase {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new EntityContextGetter(false), false },
                {new EntityContextGetter(false), true },
                {new EntityContextGetter(true), false }
           });
    }

    private EntityContextGetter getter;
    private EntityContext theEntityContext;

    public TestQuery(EntityContextGetter getter, boolean autoCommitMode) {
        this.getter = new EntityContextGetter(false);
        this.autoCommitMode = false;
    }

//    public TestQuery() {
//        getter = new EntityContextGetter(false);
//        autoCommitMode = false;
//    }
//
    @Override
    protected void prepareData() throws Exception {
        super.prepareData();
        executeScript("/inserts.sql", false);
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        this.theEntityContext = getter.get(this);
    }

    @Test
    public void testCsvSyntaxModelQuery() throws Exception {
        QCsvSyntaxModel qcsm = new QCsvSyntaxModel();
        qcsm.joinToUser(INNER);
        qcsm.joinToStructure(INNER).joinToFields(LEFT_OUTER);

        QCsvStructure aStructure = qcsm.existsStructure();
        qcsm.where(qcsm.name().equal("John"));
        qcsm.orExists(aStructure.where(aStructure.name().equal("csv-str-1")));
        qcsm.or(qcsm.syntaxType().equal(SyntaxType.ROOT));
        qcsm.or(qcsm.syntaxType().equal(SyntaxType.SUBSYNTAX));

        QueryResult<CsvSyntaxModel> result = theEntityContext.performQuery(qcsm);

        System.out.println();
        System.out.println("printing syntax models (" + result.getList().size() + ") => ");
        assertEquals(2, result.getList().size());
        for (CsvSyntaxModel syntaxModel : result.getList()) {
            print("", syntaxModel);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

   @Test
   public void testSyntaxModelQueryResultsCount() throws Exception {
      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();

      /*
       * create and registery all fetch queries
       */
      QXmlSyntaxModel qxsm = new QXmlSyntaxModel();
      qxsm.joinToStructure();
      qxsm.joinToUser();
      qxsm.joinToMappings();

      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();

      /*
       * Execute the query and process the result
       */
      QueryResult<XmlSyntaxModel> result = theEntityContext.performQuery(qxsm);
      assertEquals(2, result.getList().size());

      //result.getList().get(0).getMappings();

      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println();
      System.out.println("printing syntax models (" + result.getList().size() + ") => ");
      for (XmlSyntaxModel syntaxModel : result.getList()) {
         print("", syntaxModel);
      }

      System.out.println("printing again no fetching this time) => ");
      for (XmlSyntaxModel syntaxModel : result.getList()) {
         print("", syntaxModel);
      }
      /*
       * check the server auto-commit mode hasn't changed somehow
       */
      assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
   }


   @Test
    public void testSyntaxModelComplexQuery() throws Exception {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        /*
         * create and registery all fetch queries
         */
        QXmlSyntaxModel qxsm = new QXmlSyntaxModel();
        qxsm.joinToStructure();
        qxsm.joinToUser();
//        QXmlSyntaxModel subSyntax = qxsm.joinToMappings().joinToSubSyntax();
//        subSyntax.joinToUser();
//        subSyntax.joinToStructure();
//        subSyntax.joinToMappings();


        QXmlMapping qm =  new QXmlMapping();
        QXmlSyntaxModel qs = qm.joinToSubSyntax();
        qs.joinToUser();
        qs.joinToStructure();
        qs.joinToMappings();

        theEntityContext.register(qxsm);
        theEntityContext.register(qm);

        /*
         * get a copy of the syntax query
         */
        QXmlSyntaxModel syntax = (QXmlSyntaxModel) theEntityContext.getQuery(XmlSyntaxModel.class);

        /*
         * add a where clause
         */
        QXmlMapping aMapping = syntax.existsMappings();
        QUser aUser = syntax.existsUser();
        //QXmlStructure aStructure = syntax.existsStructure();
        syntax.where(syntax.name().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.name().equal("Scott")));

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        /*
         * Execute the query and process the result
         */
        QueryResult<XmlSyntaxModel> result = theEntityContext.performQuery(syntax);
        assertEquals(1, result.getList().size());

        //result.getList().get(0).getMappings();

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("printing syntax models (" + result.getList().size() + ") => ");
        for (XmlSyntaxModel syntaxModel : result.getList()) {
            print("", syntaxModel);
        }

        System.out.println("printing again no fetching this time) => ");
        for (XmlSyntaxModel syntaxModel : result.getList()) {
            print("", syntaxModel);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }


    @Test
    public void testStreamingSyntaxModelComplexQuery() throws Exception {
      if (getter.testingRemoteClient()) {
        return;
      }
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        /*
         * create and registery all fetch queries
         */
        QXmlSyntaxModel qxsm = new QXmlSyntaxModel();
        qxsm.joinToStructure();
        qxsm.joinToUser();
        QXmlSyntaxModel subSyntax = qxsm.joinToMappings().joinToSubSyntax();
        subSyntax.joinToUser();
        subSyntax.joinToStructure();
        subSyntax.joinToMappings();


        theEntityContext.register(qxsm);

        /*
         * get a copy of the syntax query
         */
        QXmlSyntaxModel syntax = (QXmlSyntaxModel) theEntityContext.getQuery(XmlSyntaxModel.class);

        /*
         * add a where clause
         */
        QXmlMapping aMapping = syntax.existsMappings();
        QUser aUser = syntax.existsUser();
        //QXmlStructure aStructure = syntax.existsStructure();
        syntax.where(syntax.name().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.name().equal("Scott")));

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        try {
            /*
             * Execute the query and process the result
             */
            try (ObjectInputStream<XmlSyntaxModel> in =  theEntityContext.streamObjectQuery(syntax); ) {
                XmlSyntaxModel model;
                int count = 0;
                while((model = in.read()) != null) {
                    count++;
                    print("", model);
               }
                assertEquals(1, count);
            }
        }
        catch(UnsupportedOperationException x) {
            assertTrue("Remote streaming is not supported", getter.client);
        }
    }

    @Test
    public void testStreamSupportSyntaxModelComplexQuery() throws Exception {
      if (getter.testingRemoteClient()) {
        return;
      }
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        /*
         * create and registery all fetch queries
         */
        QXmlSyntaxModel qxsm = new QXmlSyntaxModel();
        qxsm.joinToStructure();
        qxsm.joinToUser();
//        QXmlSyntaxModel subSyntax = qxsm.joinToMappings().joinToSubSyntax();
//        subSyntax.joinToUser();
//        subSyntax.joinToStructure();
//        subSyntax.joinToMappings();


     //   theEntityContext.register(qxsm);

        /*
         * get a copy of the syntax query
         */
        QXmlSyntaxModel syntax = (QXmlSyntaxModel) theEntityContext.getQuery(XmlSyntaxModel.class);

        /*
         * add a where clause
         */
        QXmlMapping aMapping = syntax.existsMappings();
        QUser aUser = syntax.existsUser();
        //QXmlStructure aStructure = syntax.existsStructure();
        syntax.where(syntax.name().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.name().equal("Scott")));

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        try {
            /*
             * Execute the query and process the result
             */
            try (Stream<XmlSyntaxModel> stream =  theEntityContext.streamObjectQuery(syntax).stream(); ) {
                stream
                    .map(XmlSyntaxModel::streamMappings)
                    .flatMap(DataStream::stream)
                    .map(XmlMapping::getSubSyntax)
                    .filter(Objects::nonNull)
                    .map(XmlSyntaxModel::streamMappings)
                    .flatMap(DataStream::stream)
                    .forEach(m -> print("", m));
            }
        }
        catch(UnsupportedOperationException x) {
            assertTrue("Remote streaming is not supported", getter.client);
        }
    }

    @Test
    public void testStreamingDownSyntaxModelHierarchy() throws Exception {
      if (getter.testingRemoteClient()) {
        return;
      }

        QXmlSyntaxModel query = new QXmlSyntaxModel();
        query.where(query.name().equal("syntax-xml-1"));

        //stream the query result.
        try (ObjectInputStream<XmlSyntaxModel> syntaxModelStream =  theEntityContext.streamObjectQuery( query ); ) {

            XmlSyntaxModel syntaxModel;
            while((syntaxModel = syntaxModelStream.read()) != null) {
                Runtime.getRuntime().gc();
                System.out.println("-------------------------------------------");
                System.out.println("**** NAME:" + syntaxModel.getName());

                //stream all of the mappings for the syntax
                try (ObjectInputStream<XmlMapping> mappingsStream = syntaxModel.streamMappings()) {
                    XmlMapping mapping;
                    while((mapping = mappingsStream.read()) != null) {
                        System.out.println("-------------------------------------------");
                        System.out.println("****XPATH : " + mapping.getXpath());
                        System.out.println("****TARGET: " + mapping.getTargetFieldName());
                        Runtime.getRuntime().gc();

                        //will cause  a lazy load
                        XmlSyntaxModel subSyntax = mapping.getSubSyntax();
                        if (subSyntax != null) {
                            System.out.println("-------------------------------------------");
                            System.out.println(subSyntax.getName());

                            //stream all of the mappings for the subyntax
                            try (ObjectInputStream<XmlMapping> mappingsSubStream = subSyntax.streamMappings()) {
                                XmlMapping mappingSub;
                                while((mappingSub = mappingsSubStream.read()) != null) {
                                    System.out.println("-------------------------------------------");
                                    System.out.println("****XPATH : " + mappingSub.getXpath());
                                    System.out.println("****TARGET: " + mappingSub.getTargetFieldName());
                                    Runtime.getRuntime().gc();
                                }
                            }
                        }
                        Runtime.getRuntime().gc();
                    }
                }
            }
        }

        Runtime.getRuntime().gc();
        Thread.sleep(500);
        Runtime.getRuntime().gc();
    }

    @Test
    public void testStreamingDownSyntaxModelHierarchyWithJoins() throws Exception {
      if (getter.testingRemoteClient()) {
        return;
      }

        QXmlSyntaxModel query = new QXmlSyntaxModel();
        query.where(query.name().equal("syntax-xml-1"));

        //stream the query result.
        try (ObjectInputStream<XmlSyntaxModel> syntaxModelStream =  theEntityContext.streamObjectQuery( query ); ) {

            XmlSyntaxModel syntaxModel;
            while((syntaxModel = syntaxModelStream.read()) != null) {
                Runtime.getRuntime().gc();
                System.out.println("-------------------------------------------");
                System.out.println("**** NAME:" + syntaxModel.getName());

                /*
                 * we define a custom fetch graph and use it when streaming the mappings below.
                 */
                QXmlMapping qmappings = new QXmlMapping();
                qmappings.joinToSubSyntax();

                //stream all of the mappings for the syntax
                try (ObjectInputStream<XmlMapping> mappingsStream = syntaxModel.streamMappings( qmappings )) {
                    XmlMapping mapping;
                    while((mapping = mappingsStream.read()) != null) {
                        System.out.println("-------------------------------------------");
                        System.out.println("****XPATH : " + mapping.getXpath());
                        System.out.println("****TARGET: " + mapping.getTargetFieldName());
                        Runtime.getRuntime().gc();

                        XmlSyntaxModel subSyntax = mapping.getSubSyntax();
                        if (subSyntax != null) {
                            System.out.println("-------------------------------------------");
                            System.out.println(subSyntax.getName());

                            //stream all of the mappings for the subyntax
                            try (ObjectInputStream<XmlMapping> mappingsSubStream = subSyntax.streamMappings()) {
                                XmlMapping mappingSub;
                                while((mappingSub = mappingsSubStream.read()) != null) {
                                    System.out.println("-------------------------------------------");
                                    System.out.println("****XPATH : " + mappingSub.getXpath());
                                    System.out.println("****TARGET: " + mappingSub.getTargetFieldName());
                                    Runtime.getRuntime().gc();
                                }
                            }
                        }
                        Runtime.getRuntime().gc();
                    }
                }
            }
        }

        Runtime.getRuntime().gc();
        Thread.sleep(500);
        Runtime.getRuntime().gc();
    }


    /**
     * Loads all ROOT syntaxes in one abstract query (XML + CSV)
     * Only concrete proxies are instantiated (XML + CSV) as the base proxy is
     * an abstract class which fundamentally cannot be instantiated.
     * @throws Exception
     */
    @Test
    public void testBaseSyntaxModels() throws Exception {
        QSyntaxModel qsyntax = new QSyntaxModel();
        qsyntax.where(qsyntax.syntaxType().equal(SyntaxType.ROOT));
        qsyntax.joinToUser();

        List<SyntaxModel> syntaxModels = theEntityContext.performQuery(qsyntax).getList();
        for (SyntaxModel syntaxModel : syntaxModels) {
            //TODO: add syntaxModel.getStructure().getName(); back in
            //syntaxModel.getStructure().getName();
//            System.out.println(syntaxModel.getName() + " -- " + syntaxModel.getUser().getName() + " -- " + syntaxModel.getStructure().getName());
        }
        for (SyntaxModel syntaxModel : syntaxModels) {
            print("", syntaxModel);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testSyntaxModelLazyName() throws Exception {
        /*
         * create and registery all fetch queries
         */
        QXmlSyntaxModel qxsm = new QXmlSyntaxModel();
        qxsm.joinToStructure();
        QUser quser = qxsm.joinToUser();
        QXmlSyntaxModel sub = qxsm.joinToMappings().joinToSubSyntax();
        sub.joinToStructure();
        sub.joinToUser();
        sub.joinToMappings();

        qxsm.select(qxsm.syntaxType());
        quser.select(quser.name());
        sub.select(sub.name(), sub.syntaxType());


        /*
         * get a copy of the syntax query
         */
        List<XmlSyntaxModel> list = theEntityContext.performQuery(qxsm).getList();
        System.out.println(theEntityContext.printXml());
        for (XmlSyntaxModel syntaxModel : list) {
            print("", syntaxModel);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testQueryTemplateAndDatatypeEagerLoading() throws Exception {
        /*
         * fetching over a join table
         */
        QTemplate templatesQuery = new QTemplate();
        templatesQuery.joinToContents();
        templatesQuery.joinToBusinessType();

        QueryResult<Template> result2 = theEntityContext.performQuery(templatesQuery);
        for (Template t : result2.getList()) {
            print("", t);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testQueryTemplateAndDatatypeWithStreaming() throws Exception {
      if (getter.testingRemoteClient()) {
        return;
      }

        /*
         * fetching over a join table
         */
        QTemplate templatesQuery = new QTemplate();

        QueryResult<Template> result2 = theEntityContext.performQuery(templatesQuery);
        for (Template t : result2.getList()) {
            QTemplateBusinessType qtb = new QTemplateBusinessType();
            qtb.joinToBusinessType();
            try ( ObjectInputStream<TemplateBusinessType> in = t.streamBusinessTypes(qtb);) {
                TemplateBusinessType tbt;
                while((tbt = in.read()) != null) {
                    System.out.println(tbt.getBusinessType().getName());
                }
            }
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testQueryTemplateAndDatatypeFetchesOverJoinTable() throws Exception {
        /*
         * fetching over a join table
         */
        QTemplate templatesQuery = new QTemplate();
        //templatesQuery.joinToDatatype();

        QueryResult<Template> result2 = theEntityContext.performQuery(templatesQuery);
        for (Template t : result2.getList()) {
            print("", t);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testBatchQuery() throws Exception {
        /*
         * Build a syntax model query
         */
        QXmlSyntaxModel syntax = (QXmlSyntaxModel) theEntityContext.getDefinitions().getQuery(XmlSyntaxModel.class);
        QXmlMapping aMapping = syntax.existsMappings();
        QUser aUser = syntax.existsUser();
        syntax.where(syntax.name().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.name().equal("Scott")));

        /*
         * Build a template query
         */
        QTemplate templatesQuery = new QTemplate();
        templatesQuery.joinToBusinessType();

        QueryBatcher qBatch = new QueryBatcher();
        qBatch.addQuery(syntax, templatesQuery);

        theEntityContext.performQueries(qBatch);

        System.out.println();
        System.out.println();
        System.out.println("Printing SyntaxModel models");

        for (XmlSyntaxModel syntaxModel : qBatch.getResult(0, XmlSyntaxModel.class).getList()) {
            print("", syntaxModel);
        }

        System.out.println();
        System.out.println("Printing Templates");

        for (Template template : qBatch.getResult(1, Template.class).getList()) {
            print("", template);
        }
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

    @Test
    public void testNullQueryParameter() throws Exception {
        QSyntaxModel qsyntax = new QSyntaxModel();
        qsyntax.where(qsyntax.syntaxType().isNull());
        qsyntax.joinToUser();

        assertTrue(theEntityContext.performQuery(qsyntax).getList().isEmpty());
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

}
