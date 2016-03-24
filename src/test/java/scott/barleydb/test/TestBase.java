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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberInputStream;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.example.acl.model.AccessArea;
import org.example.acl.model.MacProxyFactory;
import org.example.acl.model.User;
import org.example.acl.query.QAccessArea;
import org.example.acl.query.QUser;
import org.example.etl.EtlSpec;
import org.example.etl.context.MiEntityContext;
import org.example.etl.model.BusinessType;
import org.example.etl.model.CsvMapping;
import org.example.etl.model.CsvStructure;
import org.example.etl.model.CsvStructureField;
import org.example.etl.model.CsvSyntaxModel;
import org.example.etl.model.MiProxyFactory;
import org.example.etl.model.SyntaxModel;
import org.example.etl.model.Template;
import org.example.etl.model.TemplateContent;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlStructure;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QBusinessType;
import org.example.etl.query.QCsvMapping;
import org.example.etl.query.QCsvStructure;
import org.example.etl.query.QCsvStructureField;
import org.example.etl.query.QCsvSyntaxModel;
import org.example.etl.query.QRawData;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QTemplateBusinessType;
import org.example.etl.query.QTemplateContent;
import org.example.etl.query.QXmlMapping;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.model.SyntaxType;
import org.junit.After;
import org.junit.Before;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.query.RuntimeProperties.Concurrency;
import scott.barleydb.api.query.RuntimeProperties.ScrollType;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.vendor.MySqlSpecConverter;
import scott.barleydb.server.jdbc.converter.LongToStringTimestampConverter;
import scott.barleydb.server.jdbc.persist.SequenceGenerator;
import scott.barleydb.server.jdbc.query.QueryPreProcessor;
import scott.barleydb.server.jdbc.vendor.HsqlDatabase;
import scott.barleydb.server.jdbc.vendor.MySqlDatabase;
import scott.barleydb.server.jdbc.vendor.OracleDatabase;
import scott.barleydb.server.jdbc.vendor.SqlServerDatabase;

@SuppressWarnings("deprecation")
public abstract class TestBase {

    public interface DatabaseTestSetup  {
        String getDriverClassName();
        String getUrl();
        String getUser();
        String getPassword();
        String getSchemaName();
        Properties getConnectionProperties();
    }

    public static class HsqlDbTest implements DatabaseTestSetup {
        @Override
        public String getDriverClassName() {
            return "org.hsqldb.jdbcDriver";
        }

        @Override
        public String getUrl() {
            return "jdbc:hsqldb:mem:testdb";
        }

        @Override
        public String getUser() {
            return "sa";
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getSchemaName() {
            return "hsqldb-schema.sql";
        }

        @Override
        public Properties getConnectionProperties() {
            Properties props = new Properties();
            return props;
        }
    }

    public static class MySqlDbTest implements DatabaseTestSetup {
        @Override
        public String getDriverClassName() {
            return "com.mysql.jdbc.Driver";
        }

        @Override
        public String getUrl() {
            return "jdbc:mysql://localhost:3306/sort";
        }

        @Override
        public String getUser() {
            return "root";
        }

        @Override
        public String getPassword() {
            return "s1ncla1r12";
        }

        @Override
        public String getSchemaName() {
            return "mysql-schema.sql";
        }

        @Override
        public Properties getConnectionProperties() {
            Properties props = new Properties();
            props.setProperty("CLIENT_MULTI_STATEMENTS", "true");
            return props;
        }
    }


    private static boolean databaseInitialized = false;
    protected static Environment env;
    protected static TestEntityContextServices entityContextServices;
    protected static String transformXml;
    protected static DatabaseTestSetup db = new HsqlDbTest();

    protected static final String namespace = "org.example.etl";
    protected static DataSource dataSource;
    protected EntityContext serverEntityContext;
    protected boolean autoCommitMode = false;

    protected void prepareData() throws Exception {
      executeScript("/clean.sql", false);
    }

    private void initDb() throws Exception {
        if (!databaseInitialized) {
            DriverManagerDataSource dmDataSource = new DriverManagerDataSource();
            dmDataSource.setDriverClassName( db.getDriverClassName());
            dmDataSource.setUrl( db.getUrl());
            dmDataSource.setUsername( db.getUser() );
            dmDataSource.setPassword( db.getPassword() );
            dmDataSource.setConnectionProperties( db.getConnectionProperties() );
            dataSource = dmDataSource;

            if (db instanceof HsqlDbTest) {
            executeScript("/drop.sql", true);
            executeScript("/" + db.getSchemaName(), false);
           }
            databaseInitialized = true;
        }
    }

    public static void executeScript(String script, boolean continueOnError) throws Exception {
        System.out.println("EXECUTING SCRIPT " + script);
        LineNumberReader in = new LineNumberReader(new InputStreamReader(new ClassPathResource(script).getInputStream(), "UTF-8"));
        List<String> statements = new LinkedList<>();
        JdbcTestUtils.splitSqlScript(JdbcTestUtils.readScript(in), ';', statements);
        try (Connection c = dataSource.getConnection(); ) {
            c.setAutoCommit(false);
            try ( Statement s = c.createStatement(); ) {
                for (String line: statements) {
                    try {
                        s.addBatch(line);
                    }
                    catch(Exception x) {
                        if (!continueOnError) {
                            c.rollback();
                            throw x;
                        }
                        System.err.println(x.getMessage());
                    }
                }
                try {
                    s.executeBatch();
                }
                catch(Exception x) {
                    if (!continueOnError) {
                        c.rollback();
                        throw x;
                    }
                    System.err.println(x.getMessage());
                }
                c.commit();
            }
        }
        catch(Exception x) {
            if (!continueOnError) throw x;
        }
        System.out.println("FINISHED EXECUTING SCRIPT " + script);
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
                new SqlServerDatabase(metadata),
                new MySqlDatabase(metadata));

        connection.close();

        class TestSequenceGenerator implements SequenceGenerator {
            Long key = 1l;

            @Override
            public Object getNextKey(EntityType entityType) {
                return key++;
            }
        };

        entityContextServices.setSequenceGenerator(new TestSequenceGenerator());
        entityContextServices.register(new LongToStringTimestampConverter());


        env.addDefinitions( Definitions.create( loadDefinitions("src/test/java/org/example/acl/aclspec.xml", "org.example.acl") ) );
        env.addDefinitions( Definitions.create( loadDefinitions("src/test/java/org/example/etl/etlspec.xml", "org.example.etl") ) );

        env.getDefinitions("org.example.acl").registerQueries(
                new QUser(),
                new QAccessArea());

        env.getDefinitions("org.example.acl").registerProxyFactory(new MacProxyFactory());

        env.getDefinitions("org.example.etl").registerQueries(
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
                new QBusinessType(),
                new QRawData());

        env.getDefinitions("org.example.etl").registerProxyFactory(new MiProxyFactory());

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
        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class, StructureType.class, SyntaxType.class, EtlSpec.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        SpecRegistry registry = (SpecRegistry)unmarshaller.unmarshal(new File(path));
        DefinitionsSpec spec = registry.getDefinitionsSpec(namespace);
        if (spec == null) {
            throw new IllegalStateException("Could not load definitions " + namespace);
        }

        if (db instanceof MySqlDbTest) {
            spec = MySqlSpecConverter.convertSpec( spec );
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
