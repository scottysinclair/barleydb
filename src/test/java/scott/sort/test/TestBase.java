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
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
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
import scott.sort.api.persist.PersistAnalyser;
import scott.sort.api.query.RuntimeProperties;
import scott.sort.api.query.RuntimeProperties.Concurrency;
import scott.sort.api.query.RuntimeProperties.ScrollType;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.SpecRegistry;
import scott.sort.server.jdbc.persist.SequenceGenerator;
import scott.sort.server.jdbc.query.QueryPreProcessor;
import scott.sort.server.jdbc.vendor.HsqlDatabase;
import scott.sort.server.jdbc.vendor.OracleDatabase;
import scott.sort.server.jdbc.vendor.SqlServerDatabase;

import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.model.MacProxyFactory;
import com.smartstream.mac.model.User;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mac.query.QUser;
import com.smartstream.mi.MiSpec;
import com.smartstream.mi.context.MiEntityContext;
import com.smartstream.mi.model.CsvMapping;
import com.smartstream.mi.model.CsvStructure;
import com.smartstream.mi.model.CsvStructureField;
import com.smartstream.mi.model.CsvSyntaxModel;
import com.smartstream.mi.model.BusinessType;
import com.smartstream.mi.model.MiProxyFactory;
import com.smartstream.mi.model.SyntaxModel;
import com.smartstream.mi.model.Template;
import com.smartstream.mi.model.TemplateContent;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.model.XmlStructure;
import com.smartstream.mi.model.XmlSyntaxModel;
import com.smartstream.mi.query.QCsvMapping;
import com.smartstream.mi.query.QCsvStructure;
import com.smartstream.mi.query.QCsvStructureField;
import com.smartstream.mi.query.QCsvSyntaxModel;
import com.smartstream.mi.query.QBusinessType;
import com.smartstream.mi.query.QTemplate;
import com.smartstream.mi.query.QTemplateContent;
import com.smartstream.mi.query.QTemplateBusinessType;
import com.smartstream.mi.query.QXmlMapping;
import com.smartstream.mi.query.QXmlStructure;
import com.smartstream.mi.query.QXmlSyntaxModel;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;

@SuppressWarnings("deprecation")
public abstract class TestBase {

    private static boolean databaseInitialized = false;
    protected static Environment env;
    protected static TestEntityContextServices entityContextServices;
    protected static String transformXml;

    protected static final String namespace = "com.smartstream.mi";
    protected static DataSource dataSource;
    protected EntityContext serverEntityContext;
    protected boolean autoCommitMode = false;

    protected void prepareData() {
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/clean.sql"), false);
    }

    private void initDb() {
        if (!databaseInitialized) {
            DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
            dmDataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dmDataSource.setUrl("jdbc:hsqldb:mem:testdb");
            //dmDataSource.setUrl( "jdbc:hsqldb:mem:testdb;close_result=true;hsqldb.applog=3;hsqldb.sqllog=3");
            dmDataSource.setUsername("sa");
            dmDataSource.setPassword("");
            dataSource = dmDataSource;

            SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(dataSource), new ClassPathResource("/schema.sql"), false);
            databaseInitialized = true;
        }
    }

    public static void setupDefs() throws Exception {
        if (env != null) {
            return;
        }

        entityContextServices = new TestEntityContextServices(dataSource);
        env = new Environment(entityContextServices);
        /*
         * The server executes by default in the same context
         * and provides reasonable values for result-set scrolling and fetching
         */
        env.setDefaultRuntimeProperties(
                new RuntimeProperties()
                    .concurrency(Concurrency.READ_ONLY)
                    .fetchSize(100)
                    .executeInSameContext(true)
                    .scrollType(ScrollType.FORWARD_ONLY));
        env.setQueryPreProcessor(new QueryPreProcessor());

        entityContextServices.setEnvironment(env);
        env.loadDefinitions();

        Connection connection = dataSource.getConnection();
        DatabaseMetaData metadata = connection.getMetaData();
        entityContextServices.addDatabases(
                new HsqlDatabase(metadata),
                new OracleDatabase(metadata),
                new SqlServerDatabase(metadata));

        connection.close();

        class TestSequenceGenerator implements SequenceGenerator {
            Long key = 1l;

            @Override
            public Object getNextKey(EntityType entityType) {
                return key++;
            }
        };
        entityContextServices.setSequenceGenerator(new TestSequenceGenerator());

        env.addDefinitions( Definitions.create( loadDefinitions("src/test/java/com/smartstream/mac/macspec.xml", "com.smartstream.mac") ) );
        env.addDefinitions( Definitions.create( loadDefinitions("src/test/java/com/smartstream/mi/mispec.xml", "com.smartstream.mi") ) );

        env.getDefinitions("com.smartstream.mac").registerQueries(
                new QUser(),
                new QAccessArea());

        env.getDefinitions("com.smartstream.mac").registerProxyFactory(new MacProxyFactory());

        env.getDefinitions("com.smartstream.mi").registerQueries(
                new QXmlSyntaxModel(),
                new QXmlStructure(),
                new QXmlMapping(),
                new QCsvSyntaxModel(),
                new QCsvStructure(),
                new QCsvStructureField(),
                new QCsvMapping(),
                new QTemplate(),
                new QTemplateContent(),
                new QTemplateBusinessType(),
                new QBusinessType());

        env.getDefinitions("com.smartstream.mi").registerProxyFactory(new MiProxyFactory());

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

        /*
         * create a session with the definitions above, the query registry and a datasource.
         */
        serverEntityContext = new MiEntityContext(env);
        serverEntityContext.setAutocommit( autoCommitMode );
    }

    @After
    public void tearDown() throws Exception {
        if (!serverEntityContext.getAutocommit()) {
            serverEntityContext.rollback();
        }
    }

    private static DefinitionsSpec loadDefinitions(String path, String namespace) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, MiSpec.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        SpecRegistry registry = (SpecRegistry)unmarshaller.unmarshal(new File(path));
        DefinitionsSpec spec = registry.getDefinitionsSpec(namespace);
        if (spec == null) {
            throw new IllegalStateException("Could not load definitions " + namespace);
        }
        return spec;
    }

    protected Entity toEntity(Object object) {
        return ((ProxyController) object).getEntity();
    }

    protected void printEntityContext(EntityContext entityContext) throws Exception {
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
        if (template.getBusinessTypes() != null) {
            for (BusinessType dt : template.getBusinessTypes()) {
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

    protected static void print(String prefix, BusinessType datatype) {
        System.out.println(prefix + "BusinessType Id   " + datatype.getId());
        System.out.println(prefix + "BusinessType Name " + datatype.getName());
    }

    protected static void print(String prefix, SyntaxModel syntaxModel) {
        if (syntaxModel instanceof XmlSyntaxModel) {
            print(prefix, (XmlSyntaxModel) syntaxModel);
        }
        else if (syntaxModel instanceof CsvSyntaxModel) {
            print(prefix, (CsvSyntaxModel) syntaxModel);
        }
    }

    protected static void print(String prefix, XmlSyntaxModel syntaxModel) {
        System.out.println(prefix + "XMLSyntax Id   " + syntaxModel.getId());
        System.out.println(prefix + "XMLSyntax Name " + syntaxModel.getName());
        System.out.println(prefix + "XMLSyntax JoinType " + syntaxModel.getSyntaxType());
        if (syntaxModel.getUser() != null) {
            print(prefix + "  ", syntaxModel.getUser());
        }
        if (syntaxModel.getStructure() != null) {
            print(prefix + "  ", syntaxModel.getStructure());
        }
        if (syntaxModel.getMappings() != null) {
            for (XmlMapping mapping : syntaxModel.getMappings()) {
                print(prefix + "  ", mapping);
            }
        }
    }

    protected static void print(String prefix, CsvSyntaxModel syntaxModel) {
        System.out.println(prefix + "CSVSyntax Id   " + syntaxModel.getId());
        System.out.println(prefix + "CSVSyntax Name " + syntaxModel.getName());
        System.out.println(prefix + "CSVSyntax JoinType " + syntaxModel.getSyntaxType());
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

    protected static void print(String prefix, XmlStructure structure) {
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
        System.out.println(prefix + "Mapping Column   " + mapping.getStructureField().getColumnIndex());
        System.out.println(prefix + "Mapping Target  " + mapping.getTargetFieldName());
        System.out.println(prefix + "Mapping SyntaxModel  " + mapping.getSyntax().getId());
    }

    protected static void print(String prefix, XmlMapping mapping) {
        System.out.println(prefix + "Mapping Id      " + mapping.getId());
        System.out.println(prefix + "Mapping XPath   " + mapping.getXpath());
        System.out.println(prefix + "Mapping Target  " + mapping.getTargetFieldName());
        System.out.println(prefix + "Mapping SyntaxModel  " + mapping.getSyntax().getId());
        if (mapping.getSubSyntax() != null) {
            XmlSyntaxModel sub = mapping.getSubSyntax();
            if (sub != null) {
                print(prefix + "  ", sub);
            }
        }
    }

}
