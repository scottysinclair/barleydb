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
import static org.junit.Assert.assertTrue;
import static scott.barleydb.api.query.JoinType.INNER;
import static scott.barleydb.api.query.JoinType.LEFT_OUTER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.example.acl.query.QUser;
import org.example.etl.model.CsvSyntaxModel;
import org.example.etl.model.SyntaxModel;
import org.example.etl.model.Template;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QCsvStructure;
import org.example.etl.query.QCsvSyntaxModel;
import org.example.etl.query.QSyntaxModel;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QXmlMapping;
import org.example.etl.query.QXmlSyntaxModel;
import org.example.etl.types.SyntaxType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.server.jdbc.resources.ConnectionResources;

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
        this.getter = getter;
        this.autoCommitMode = autoCommitMode;
    }
/*
    public TestQuery() {
    	getter = new EntityContextGetter(false);
    	autoCommitMode = false;
    }
    */
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
        for (CsvSyntaxModel syntaxModel : result.getList()) {
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
        qxsm.joinToUser();
        qxsm.joinToMappings()
                .joinToSubSyntax()
                   .joinToUser();

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

        /*
         * Execute the query and process the result
         */
        QueryResult<XmlSyntaxModel> result = theEntityContext.performQuery(syntax);

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
        qsyntax.where(qsyntax.syntaxType().equal(null));
        qsyntax.joinToUser();

        assertTrue(theEntityContext.performQuery(qsyntax).getList().isEmpty());
        /*
         * check the server auto-commit mode hasn't changed somehow
         */
        assertEquals(autoCommitMode, serverEntityContext.getAutocommit());
    }

}
