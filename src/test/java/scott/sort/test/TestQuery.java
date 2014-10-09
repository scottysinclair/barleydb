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

import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.sort.api.core.QueryBatcher;
import scott.sort.server.jdbc.queryexecution.QueryResult;

import com.smartstream.mac.query.QUser;
import com.smartstream.messaging.model.*;
import com.smartstream.messaging.query.*;

@SuppressWarnings("deprecation")
public class TestQuery extends TestBase {

    @Override
    protected void prepareData() {
        super.prepareData();
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/inserts.sql"), false);
    }

    @Test
    public void testCsvSyntaxModelQuery() throws Exception {

        QCsvSyntaxModel qcsm = new QCsvSyntaxModel();
        qcsm.joinToUser();
        qcsm.joinToStructure().joinToFields();

        QCsvStructure aStructure = qcsm.existsStructure();
        qcsm.whereExists(aStructure.where(aStructure.name().equal("csv-str-1")));

        QueryResult<CsvSyntaxModel> result = entityContext.performQuery(qcsm);

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("printing syntax models (" + result.getList().size() + ") => ");
        for (CsvSyntaxModel syntaxModel : result.getList()) {
            print("", syntaxModel);
        }
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

        entityContext.register(qxsm);

        /*
         * get a copy of the syntax query
         */
        QXMLSyntaxModel syntax = (QXMLSyntaxModel) entityContext.getQuery(XMLSyntaxModel.class);

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
        QueryResult<XMLSyntaxModel> result = entityContext.performQuery(syntax);

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

        List<SyntaxModel> syntaxModels = entityContext.performQuery(qsyntax).getList();
        for (SyntaxModel syntaxModel : syntaxModels) {
            syntaxModel.getStructure().getName();
            System.out.println(syntaxModel.getName() + " -- " + syntaxModel.getUser().getName() + " -- " + syntaxModel.getStructure().getName());
        }
        for (SyntaxModel syntaxModel : syntaxModels) {
            print("", syntaxModel);
        }
    }

    @Test
    public void testSyntaxModelLazyName() throws Exception {
        /*
         * create and registery all fetch queries
         */
        QXMLSyntaxModel qxsm = new QXMLSyntaxModel();
        qxsm.joinToStructure();
        qxsm.joinToUser();
        QXMLSyntaxModel sub = qxsm.joinToMappings().joinToSubSyntax();
        sub.joinToStructure();
        sub.joinToUser();
        sub.joinToMappings();

        qxsm.disableName();
        sub.disableName();

        /*
         * get a copy of the syntax query
         */
        List<XMLSyntaxModel> list = entityContext.performQuery(qxsm).getList();
        System.out.println(entityContext.printXml());
        for (XMLSyntaxModel syntaxModel : list) {
            print("", syntaxModel);
        }
    }

    @Test
    public void testQueryTemplateAndDatatypeEagerLoading() throws Exception {
        /*
         * fetching over a join table
         */
        QTemplate templatesQuery = new QTemplate();
        templatesQuery.joinToContent();
        templatesQuery.joinToDatatype();

        QueryResult<Template> result2 = entityContext.performQuery(templatesQuery);
        for (Template t : result2.getList()) {
            print("", t);
        }
    }

    @Test
    public void testQueryTemplateAndDatatypeFetchesOverJoinTable() throws Exception {
        /*
         * fetching over a join table
         */
        QTemplate templatesQuery = new QTemplate();
        //templatesQuery.joinToDatatype();

        QueryResult<Template> result2 = entityContext.performQuery(templatesQuery);
        for (Template t : result2.getList()) {
            print("", t);
        }
    }

    @Test
    public void testBatchQuery() throws Exception {
        /*
         * Build a syntax model query
         */
        QXMLSyntaxModel syntax = (QXMLSyntaxModel) entityContext.getDefinitions().getQuery(XMLSyntaxModel.class);
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

        entityContext.performQueries(qBatch);

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
    }

    @Test
    public void testNullQueryParameter() throws Exception {
        QSyntaxModel qsyntax = new QSyntaxModel();
        qsyntax.where(qsyntax.syntaxType().equal(null));
        qsyntax.joinToUser();

        assertTrue(entityContext.performQuery(qsyntax).getList().isEmpty());

    }

}