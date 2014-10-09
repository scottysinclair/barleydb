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

import java.io.ByteArrayInputStream;
import java.sql.Connection;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Before;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.server.EntityContextServices;
import scott.sort.server.jdbc.persister.*;
import scott.sort.server.jdbc.queryexecution.QueryPreProcessor;

import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.model.MacProxyFactory;
import com.smartstream.mac.model.User;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mac.query.QUser;
import com.smartstream.messaging.model.*;
import com.smartstream.messaging.query.*;

@SuppressWarnings("deprecation")
public abstract class TestBase {

    private static boolean databaseInitialized = false;
    protected static Environment env;
    protected static String transformXml;

    protected static final String namespace = "com.smartstream.messaging";
    protected static DataSource dataSource;
    protected EntityContext entityContext;

    protected void prepareData() {
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/clean.sql"), false);
    }

    private void initDb() {
        DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
        dmDataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        dmDataSource.setUrl("jdbc:hsqldb:mem:testdb");
        //dmDataSource.setUrl( "jdbc:hsqldb:mem:testdb;close_result=true;hsqldb.applog=3;hsqldb.sqllog=3");
        dmDataSource.setUsername("sa");
        dmDataSource.setPassword("");
        dataSource = dmDataSource;
        if (!databaseInitialized) {
            SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/schema.sql"), false);
            databaseInitialized = true;
        }
    }

    public static void setupDefs() throws Exception {
        if (env != null) {
            return;
        }

        EntityContextServices services = new EntityContextServices(dataSource);
        env = new Environment(services);
        env.setQueryPreProcessor(new QueryPreProcessor());
        services.setEnvironment(env);

        class TestSequenceGenerator implements SequenceGenerator {
            Long key = 1l;

            @Override
            public Object getNextKey(EntityType entityType) {
                return key++;
            }
        }
        ;
        env.setSequenceGenerator(new TestSequenceGenerator());

        /*
         * generate an entity configuration using the dsl
         */
        env.addDefinitions(Definitions.start("com.smartstream.mac")
                .newEntity(User.class, "MAC_USER")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "USER_NAME", JdbcType.VARCHAR)
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity(AccessArea.class, "MAC_ACCESS_AREA")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withOne("parent", AccessArea.class, "PARENT_ID", JdbcType.BIGINT)
                    .withMany("children", AccessArea.class, "parent")
                    .complete());

        env.addDefinitions(Definitions.start(namespace)
                .references("com.smartstream.mac")
                .newEntity(XMLStructure.class, "SS_XMLSTRUCTURE")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity(CsvStructure.class, "SS_CSVSTRUCTURE")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .ownsMany("fields", com.smartstream.messaging.model.CsvStructureField.class, "structure")
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity(CsvStructureField.class, "SS_CSVSTRUCTURE_FIELD")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withValue("columnIndex", JavaType.INTEGER, "COL_INDEX", JdbcType.INT)
                    .withValue("optional", JavaType.BOOLEAN, "OPTIONAL", JdbcType.INT)
                    .withOne("structure", com.smartstream.messaging.model.CsvStructure.class, "STRUCTURE_ID", JdbcType.BIGINT)

                .newAbstractEntity(SyntaxModel.class, "SS_SYNTAX_MODEL")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withEnum("syntaxType", SyntaxType.class, "SYNTAX_TYPE", JdbcType.INT)
                    .withValue("structureType", JavaType.INTEGER, "STRUCTURE_TYPE", JdbcType.INT)
                    //in the abstract syntax structure is just a value node
                    .withValue("structure", JavaType.LONG, "STRUCTURE_ID", JdbcType.BIGINT)
                    .withOne("user", com.smartstream.mac.model.User.class, "USER_ID", JdbcType.BIGINT)
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newChildEntity(XMLSyntaxModel.class, SyntaxModel.class)
                    .withFixedValue("structureType", JavaType.INTEGER, "STRUCTURE_TYPE", JdbcType.INT, 1)
                    .dependsOnOne("structure", com.smartstream.messaging.model.XMLStructure.class, "STRUCTURE_ID", JdbcType.BIGINT)
                    .ownsMany("mappings", com.smartstream.messaging.model.XMLMapping.class, "syntaxModel")

                .newEntity(XMLMapping.class, "SS_XML_MAPPING")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("xpath", JavaType.STRING, "XPATH", JdbcType.VARCHAR)
                    .withValue("target", JavaType.STRING, "TARGET_FIELD_NAME", JdbcType.VARCHAR)
                    .withOne("syntaxModel", com.smartstream.messaging.model.XMLSyntaxModel.class, "SYNTAX_MODEL_ID", JdbcType.BIGINT)
                    .ownsOne("subSyntaxModel", com.smartstream.messaging.model.XMLSyntaxModel.class, "SUB_SYNTAX_MODEL_ID", JdbcType.BIGINT)

                .newChildEntity(CsvSyntaxModel.class, SyntaxModel.class)
                    .withFixedValue("structureType", JavaType.INTEGER, "STRUCTURE_TYPE", JdbcType.INT, 2)
                    .dependsOnOne("structure", com.smartstream.messaging.model.CsvStructure.class, "STRUCTURE_ID", JdbcType.BIGINT)
                    .ownsMany("mappings", com.smartstream.messaging.model.CsvMapping.class, "syntaxModel")

                .newEntity(CsvMapping.class, "SS_CSV_MAPPING")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("columnIndex", JavaType.INTEGER, "COL_INDEX", JdbcType.INT)
                    .withValue("target", JavaType.STRING, "TARGET_FIELD_NAME", JdbcType.VARCHAR)
                    .withOne("syntaxModel", com.smartstream.messaging.model.CsvSyntaxModel.class, "SYNTAX_MODEL_ID", JdbcType.BIGINT)

                .newEntity(Template.class, "SS_TEMPLATE")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .ownsMany("contents", com.smartstream.messaging.model.TemplateContent.class, "template")
                    .ownsMany("datatypes", "com.smartstream.messaging.TemplateDatatype", "template", "datatype")
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity(TemplateContent.class, "SS_TEMPLATE_CONTENT")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withOne("template", Template.class, "TEMPLATE_ID", JdbcType.BIGINT)
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity(Datatype.class, "SS_DATATYPE")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withValue("name", JavaType.STRING, "NAME", JdbcType.VARCHAR)
                    .withOptimisticLock("modifiedAt", JavaType.LONG, "MODIFIED_AT", JdbcType.TIMESTAMP)

                .newEntity("com.smartstream.messaging.TemplateDatatype", "SS_TEMPLATE_DATATYPE")
                    .withKey("id", JavaType.LONG, "ID", JdbcType.BIGINT)
                    .withOne("template", Template.class, "TEMPLATE_ID", JdbcType.BIGINT)
                    .dependsOnOne("datatype", Datatype.class, "DATATYPE_ID", JdbcType.BIGINT)
                    .complete());

        env.getDefinitions("com.smartstream.mac").registerQueries(
                new QUser(),
                new QAccessArea());

        env.getDefinitions("com.smartstream.mac").registerProxyFactory(new MacProxyFactory());

        env.getDefinitions("com.smartstream.messaging").registerQueries(
                new QXMLSyntaxModel(),
                new QXMLStructure(),
                new QXMLMapping(),
                new QCsvSyntaxModel(),
                new QCsvStructure(),
                new QCsvStructureField(),
                new QCsvMapping(),
                new QTemplate(),
                new QTemplateContent(),
                new QTemplateDatatype(),
                new QDatatype());

        env.getDefinitions("com.smartstream.messaging").registerProxyFactory(new MessagingProxyFactory());

        /*
         * Convert to XML and print out, just for fun.
         */
        JAXBContext jc = JAXBContext.newInstance(Definitions.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(env.getDefinitions("com.smartstream.mac"), System.out);

        marshaller.marshal(env.getDefinitions(namespace), System.out);

        transformXml = "<?xml version=\"1.0\"?>" +
                "<xsl:stylesheet version=\"1.0\" " +
                "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                "<xsl:strip-space elements=\"*\" />" +
                "<xsl:output method=\"xml\" indent=\"yes\" />" +
                "" +
                "<xsl:template match=\"node() | @*\">" +
                "<xsl:copy>" +
                "<xsl:apply-templates select=\"node() | @*\" />" +
                "</xsl:copy>" +
                "</xsl:template>" +
                "</xsl:stylesheet>";
    }

    @Before
    public void setup() throws Exception {
        initDb();
        setupDefs();
        prepareData();

        env.setThreadLocalResource(Connection.class.getName(), dataSource.getConnection());

        /*
         * create a session with the definitions above, the query registry and a datasource.
         */
        entityContext = new EntityContext(env, namespace);
    }

    @After
    public void tearDown() throws Exception {
        try (Connection con = (Connection) env.getThreadLocalResource(Connection.class.getName(), false);) {
            if (con != null) {
                con.rollback();
            }
        }
    }

    protected Entity toEntity(Object object) {
        return ((ProxyController) object).getEntity();
    }

    protected void printentityContext(EntityContext entityContext) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element el = entityContext.toXml(doc);
        doc.appendChild(el);
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new ByteArrayInputStream(transformXml.getBytes("UTF-8"))));
        transformer.transform(new DOMSource(doc), new StreamResult(System.out));
        System.out.flush();
    }

    protected static void print(String prefix, AccessArea accessArea) {
        System.out.println(prefix + "AccessArea Id   " + accessArea.getId());
        System.out.println(prefix + "AccessArea Name " + accessArea.getName());
        if (accessArea.getParent() != null) {
            System.out.println(prefix + "AA Parent Id " + accessArea.getParent().getId());
        }
        for (AccessArea child : accessArea.getChildren()) {
            print(prefix + "  ", child);
        }
    }

    protected static void print(String prefix, Template template) {
        System.out.println(prefix + "Template Id   " + template.getId());
        System.out.println(prefix + "Template Name " + template.getName());
        if (template.getContents() != null) {
            for (TemplateContent tc : template.getContents()) {
                print(prefix + "  ", tc);
            }
        }
        if (template.getDatatypes() != null) {
            for (Datatype dt : template.getDatatypes()) {
                print(prefix + "  ", dt);
            }
        }
    }

    protected void printCreateAnalysis(PersistAnalyser analyser) {
        System.out.println();
        System.out.println("------------------------------------");
        System.out.println("------------ Create List ------------");
        System.out.println("-------------------------------------");
        for (Entity entity : analyser.getCreateGroup().getEntities()) {
            System.out.println(entity);
        }
        System.out.println();
    }

    protected void printUpdateAnalysis(PersistAnalyser analyser) {
        System.out.println();
        System.out.println("-------------------------------------");
        System.out.println("------------ Update List ------------");
        System.out.println("-------------------------------------");
        for (Entity entity : analyser.getUpdateGroup().getEntities()) {
            System.out.println(entity);
        }
        System.out.println();
    }

    protected void printDeleteAnalysis(PersistAnalyser analyser) {
        System.out.println();
        System.out.println("-------------------------------------");
        System.out.println("------------ Delete List ------------");
        System.out.println("-------------------------------------");
        for (Entity entity : analyser.getDeleteGroup().getEntities()) {
            System.out.println(entity);
        }
        System.out.println();
    }

    protected void printDependsOnAnalysis(PersistAnalyser analyser) {
        System.out.println();
        System.out.println("-------------------------------------");
        System.out.println("------------ Depends on List ------------");
        System.out.println("-------------------------------------");
        for (Entity entity : analyser.getDeleteGroup().getEntities()) {
            System.out.println(entity);
        }
        System.out.println();
    }

    protected void printAnalysis(PersistAnalyser analyser) {
        printCreateAnalysis(analyser);
        printUpdateAnalysis(analyser);
        printDeleteAnalysis(analyser);
        printDependsOnAnalysis(analyser);
    }

    protected static void print(String prefix, TemplateContent tc) {
        System.out.println(prefix + "Content Id   " + tc.getId());
        System.out.println(prefix + "Content Name " + tc.getName());
    }

    protected static void print(String prefix, Datatype datatype) {
        System.out.println(prefix + "Datatype Id   " + datatype.getId());
        System.out.println(prefix + "Datatype Name " + datatype.getName());
    }

    protected static void print(String prefix, SyntaxModel syntaxModel) {
        if (syntaxModel instanceof XMLSyntaxModel) {
            print(prefix, (XMLSyntaxModel) syntaxModel);
        }
        else if (syntaxModel instanceof CsvSyntaxModel) {
            print(prefix, (CsvSyntaxModel) syntaxModel);
        }
    }

    protected static void print(String prefix, XMLSyntaxModel syntaxModel) {
        System.out.println(prefix + "XMLSyntax Id   " + syntaxModel.getId());
        System.out.println(prefix + "XMLSyntax Name " + syntaxModel.getName());
        System.out.println(prefix + "XMLSyntax Type " + syntaxModel.getSyntaxType());
        if (syntaxModel.getUser() != null) {
            print(prefix + "  ", syntaxModel.getUser());
        }
        if (syntaxModel.getStructure() != null) {
            print(prefix + "  ", syntaxModel.getStructure());
        }
        if (syntaxModel.getMappings() != null) {
            for (XMLMapping mapping : syntaxModel.getMappings()) {
                print(prefix + "  ", mapping);
            }
        }
    }

    protected static void print(String prefix, CsvSyntaxModel syntaxModel) {
        System.out.println(prefix + "CSVSyntax Id   " + syntaxModel.getId());
        System.out.println(prefix + "CSVSyntax Name " + syntaxModel.getName());
        System.out.println(prefix + "CSVSyntax Type " + syntaxModel.getSyntaxType());
        if (syntaxModel.getUser() != null) {
            print(prefix + "  ", syntaxModel.getUser());
        }
        if (syntaxModel.getStructure() != null) {
            print(prefix + "  ", syntaxModel.getStructure());
        }
        if (syntaxModel.getMappings() != null) {
            for (CsvMapping mapping : syntaxModel.getMappings()) {
                print(prefix + "  ", mapping);
            }
        }
    }

    protected static void print(String prefix, User user) {
        System.out.println(prefix + "User Id   " + user.getId());
        System.out.println(prefix + "User Name " + user.getName());
    }

    protected static void print(String prefix, XMLStructure structure) {
        System.out.println(prefix + "Structure Id   " + structure.getId());
        System.out.println(prefix + "Structure Name " + structure.getName());
    }

    protected static void print(String prefix, CsvStructure structure) {
        System.out.println(prefix + "Structure Id   " + structure.getId());
        System.out.println(prefix + "Structure Name " + structure.getName());
        if (structure.getFields() != null) {
            for (CsvStructureField field : structure.getFields()) {
                print(prefix + "  ", field);
            }
        }
    }

    protected static void print(String prefix, CsvStructureField field) {
        System.out.println(prefix + "Field Id       " + field.getId());
        System.out.println(prefix + "Field Name     " + field.getName());
        System.out.println(prefix + "Field Index    " + field.getColumnIndex());
        System.out.println(prefix + "Field Optional " + field.getOptional());
    }

    protected static void print(String prefix, CsvMapping mapping) {
        System.out.println(prefix + "Mapping Id      " + mapping.getId());
        System.out.println(prefix + "Mapping Column   " + mapping.getColumnIndex());
        System.out.println(prefix + "Mapping Target  " + mapping.getTarget());
        System.out.println(prefix + "Mapping Syntax  " + mapping.getSyntaxModel().getId());
    }

    protected static void print(String prefix, XMLMapping mapping) {
        System.out.println(prefix + "Mapping Id      " + mapping.getId());
        System.out.println(prefix + "Mapping XPath   " + mapping.getXpath());
        System.out.println(prefix + "Mapping Target  " + mapping.getTarget());
        System.out.println(prefix + "Mapping Syntax  " + mapping.getSyntaxModel().getId());
        if (mapping.getSubSyntaxModel() != null) {
            XMLSyntaxModel sub = mapping.getSubSyntaxModel();
            if (sub != null) {
                print(prefix + "  ", sub);
            }
        }
    }

}