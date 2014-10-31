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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.sort.api.core.QueryBatcher;
import scott.sort.api.core.entity.EntityContext;
import static scott.sort.api.query.JoinType.*;
import scott.sort.server.jdbc.query.QueryResult;

import com.smartstream.mac.query.QUser;
import com.smartstream.mi.model.*;
import com.smartstream.mi.query.*;

/**
 * Tests various types of queries in a server environment and a remote client environment.
 *
 * Remote clients cannot set auto-commit to false which is not required for these tests.
 *
 * @author scott
 *
 */
@SuppressWarnings("deprecation")
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

    @Override
    protected void prepareData() {
        super.prepareData();
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/inserts.sql"), false);
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
        qcsm.where(qcsm.syntaxName().equal("John"));
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
        QXMLSyntaxModel qxsm = new QXMLSyntaxModel();
        qxsm.joinToUser();
        qxsm.joinToMappings()
                .joinToSubSyntax()
                .joinToUser();

        theEntityContext.register(qxsm);

        /*
         * get a copy of the syntax query
         */
        QXMLSyntaxModel syntax = (QXMLSyntaxModel) theEntityContext.getQuery(XMLSyntaxModel.class);

        /*
         * add a where clause
         */
        QXMLMapping aMapping = syntax.existsMapping();
        QUser aUser = syntax.existsUser();
        //QXMLStructure aStructure = syntax.existsStructure();
        syntax.where(syntax.syntaxName().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.userName().equal("Scott")));

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        /*
         * Execute the query and process the result
         */
        QueryResult<XMLSyntaxModel> result = theEntityContext.performQuery(syntax);

        //result.getList().get(0).getMappings();

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("printing syntax models (" + result.getList().size() + ") => ");
        for (XMLSyntaxModel syntaxModel : result.getList()) {
            print("", syntaxModel);
        }

        System.out.println("printing again no fetching this time) => ");
        for (XMLSyntaxModel syntaxModel : result.getList()) {
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
            syntaxModel.getStructure().getName();
            System.out.println(syntaxModel.getName() + " -- " + syntaxModel.getUser().getName() + " -- " + syntaxModel.getStructure().getName());
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
        QXMLSyntaxModel qxsm = new QXMLSyntaxModel();
        qxsm.joinToStructure();
        QUser quser = qxsm.joinToUser();
        QXMLSyntaxModel sub = qxsm.joinToMappings().joinToSubSyntax();
        sub.joinToStructure();
        sub.joinToUser();
        sub.joinToMappings();

        qxsm.select(qxsm.syntaxType());
        quser.select(quser.userName());
        sub.select(sub.syntaxName(), sub.syntaxType());


        /*
         * get a copy of the syntax query
         */
        List<XMLSyntaxModel> list = theEntityContext.performQuery(qxsm).getList();
        System.out.println(theEntityContext.printXml());
        for (XMLSyntaxModel syntaxModel : list) {
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
        templatesQuery.joinToContent();
        templatesQuery.joinToDatatype();

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
        QXMLSyntaxModel syntax = (QXMLSyntaxModel) theEntityContext.getDefinitions().getQuery(XMLSyntaxModel.class);
        QXMLMapping aMapping = syntax.existsMapping();
        QUser aUser = syntax.existsUser();
        syntax.where(syntax.syntaxName().equal("syntax-xml-1"))
                .andExists(aMapping.where(aMapping.xpath().equal("sfn11").or(aMapping.xpath().equal("sfn12"))))
                .andExists(aUser.where(aUser.userName().equal("Scott")));

        /*
         * Build a template query
         */
        QTemplate templatesQuery = new QTemplate();
        templatesQuery.joinToDatatype();

        QueryBatcher qBatch = new QueryBatcher();
        qBatch.addQuery(syntax, templatesQuery);

        theEntityContext.performQueries(qBatch);

        System.out.println();
        System.out.println();
        System.out.println("Printing Syntax models");

        for (XMLSyntaxModel syntaxModel : qBatch.getResult(0, XMLSyntaxModel.class).getList()) {
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
