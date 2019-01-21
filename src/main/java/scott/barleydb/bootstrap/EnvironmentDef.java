package scott.barleydb.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.io.LineNumberReader;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.jdbc.JdbcTestUtils;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.persist.AccessRightsChecker;
import scott.barleydb.api.persist.Auditor;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.query.RuntimeProperties.Concurrency;
import scott.barleydb.api.query.RuntimeProperties.ScrollType;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.ddlgen.CreateScriptOrder;
import scott.barleydb.build.specification.ddlgen.DropScriptOrder;
import scott.barleydb.build.specification.ddlgen.GenerateDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateHsqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateMySqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GeneratePostgreSQLDatabaseScript;
import scott.barleydb.build.specification.modelgen.GenerateProxyModels;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;
import scott.barleydb.build.specification.staticspec.processor.StaticDefinitionProcessor;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.converter.LongToStringTimestampConverter;
import scott.barleydb.server.jdbc.persist.SequenceGenerator;
import scott.barleydb.server.jdbc.query.QueryPreProcessor;
import scott.barleydb.server.jdbc.vendor.Database;
import scott.barleydb.server.jdbc.vendor.HsqlDatabase;
import scott.barleydb.server.jdbc.vendor.MySqlDatabase;
import scott.barleydb.server.jdbc.vendor.OracleDatabase;
import scott.barleydb.server.jdbc.vendor.PostgresqlDatabase;
import scott.barleydb.server.jdbc.vendor.SqlServerDatabase;

public class EnvironmentDef {

    public static EnvironmentDef build() {
        return new EnvironmentDef();
    }

    private DataSource dataSource;
    private List<Class<?>> specClasses = new LinkedList<>();
    private List<String> specFiles = new LinkedList<>();
    private List<SpecRegistry> specRegistries = new LinkedList<>();
    private boolean createDDL;
    private boolean dropSchema;
    private boolean classloading = true;

    private Class<? extends SequenceGenerator> sequenceGeneratorType;
    private AccessRightsChecker accessRightsChecker;
    private Auditor auditor;

    //resources created during the create() method..
    private JdbcEntityContextServices services;
    private List<DefinitionsSpec> allSpecs;

    public EnvironmentDef withDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public DataSourceDef withDataSource() {
        return new DataSourceDef();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public EnvironmentDef withSpecs(Class<?> ...specClass) {
        specClasses.addAll(Arrays.asList(specClass));
        return this;
    }

    public EnvironmentDef withSpecs(String ...specFiles) {
        this.specFiles.addAll( Arrays.asList(specFiles) );
        return this;
    }

    public EnvironmentDef withSpecs(SpecRegistry specRegistry) {
      this.specRegistries.addAll( Arrays.asList(specRegistry) );
      return this;
    }

    /**
     * only available after the environment has been built.
     * @return
     */
    public List<DefinitionsSpec> getAllSpecs() {
      return allSpecs;
    }

    public EnvironmentDef withDroppingSchema(boolean drop) {
        dropSchema = drop;
        return this;
    }

    public EnvironmentDef withSchemaCreation(boolean create) {
        createDDL = create;
        return this;
    }

    public EnvironmentDef withNoClasses() {
      classloading = false;
      return this;
    }

    public EnvironmentDef withSequenceGenerator(Class<? extends SequenceGenerator> sequenceGeneratorType) {
      this.sequenceGeneratorType = sequenceGeneratorType;
      return this;
    }

    public EnvironmentDef withAccessRightsChecker(AccessRightsChecker accessRightsChecker) {
      this.accessRightsChecker = accessRightsChecker;
      return this;
    }

    public EnvironmentDef withAuditor(Auditor auditor) {
      this.auditor = auditor;
      return this;
    }

    protected JdbcEntityContextServices createEntityContextServices(DataSource dataSource) {
        return new JdbcEntityContextServices(dataSource);
    }

    public Environment create() throws Exception {
        services = createEntityContextServices(dataSource);
        Environment env = new Environment(services);

        /*
         * common environment defaults
         */
        env.setDefaultRuntimeProperties(
                new RuntimeProperties()
                    .concurrency(Concurrency.READ_ONLY)
                    .fetchSize(100)
                    .executeInSameContext(true)
                    .scrollType(ScrollType.FORWARD_ONLY));

        /*
         * standard query preprocessor which is allways required
         */
        env.setQueryPreProcessor(new QueryPreProcessor());

        services.setEnvironment(env);

        try (Connection connection = dataSource.getConnection(); ) {
            DatabaseMetaData metadata = connection.getMetaData();
            services.addDatabases(
                    new HsqlDatabase(metadata),
                    new OracleDatabase(metadata),
                    new SqlServerDatabase(metadata),
                    new MySqlDatabase(metadata),
                    new PostgresqlDatabase(metadata));
        }


        services.register(new LongToStringTimestampConverter());

        /*
         * setup our spec (schema registry).
         */
        SpecRegistry registry = new SpecRegistry();


        /*
         * process all of the schema definitions and load them into the barleydb environment.
         */
        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        for (Class<?> specClass: specClasses) {
            processor.process((StaticDefinitions)specClass.newInstance(), registry);
        }
        allSpecs = new LinkedList<>(registry.getDefinitions());

        /*
         * process all XML files too
         *
         */
        for (String specFile: specFiles) {
            URL url = getClass().getClassLoader().getResource( specFile );
            if (url == null) {
                File file = new File(specFile);
                if (!file.exists()) {
                  throw new FileNotFoundException("Cannot find resource " + specFile);
                }
                url = file.toURI().toURL();
            }
            JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
            Unmarshaller um = jc.createUnmarshaller();
            SpecRegistry tmpRegistry = (SpecRegistry) um.unmarshal( url );
            for (DefinitionsSpec dspec: tmpRegistry.getDefinitions()) {
                registry.add(dspec);
                allSpecs.add( dspec );
            }
        }

        for (SpecRegistry reg: specRegistries) {
          for (DefinitionsSpec spec: reg.getDefinitions()) {
            registry.add(spec);
            allSpecs.add(spec);
          }
        }

        for (DefinitionsSpec spec: allSpecs) {
            env.addDefinitions( Definitions.create( spec ) );
        }


        /*
         * registery default queries for all entity specs.
         */
        if (classloading) {
          for (DefinitionsSpec spec: allSpecs) {
              for (EntitySpec entitySpec: spec.getEntitySpecs()) {
                  Class<?> queryClass = getClass().getClassLoader().loadClass(entitySpec.getQueryClassName());
                  env.getDefinitions(spec.getNamespace()).registerQueries((QueryObject<?>)queryClass.newInstance());
              }
          }

          /*
           * registery proxy factories for each namespace
           */
          for (DefinitionsSpec spec: allSpecs) {
              String className = GenerateProxyModels.getProxyFactoryFullyQuallifiedClassName(spec);
              Class<?> facClass = getClass().getClassLoader().loadClass( className );
              env.getDefinitions(spec.getNamespace()).registerProxyFactory((ProxyFactory)facClass.newInstance());
          }
        }

        if (dropSchema) {
            dropSchema();
        }

        if (createDDL) {
            createSchema();
        }
        if (sequenceGeneratorType != null) {
          SequenceGenerator seqGen = sequenceGeneratorType.getConstructor(Environment.class).newInstance(env);
          services.setSequenceGenerator(seqGen);
        }
        if (accessRightsChecker != null) {
          env.setAccessRightsChecker(accessRightsChecker);
        }
        if (auditor != null) {
          env.setAuditor(auditor);
        }

        return env;
    }

    private GenerateDatabaseScript getScriptGeneratorFor(Database dbInfo) {
        if (dbInfo instanceof HsqlDatabase) {
            return new GenerateHsqlDatabaseScript();
        }
        else if (dbInfo instanceof MySqlDatabase) {
            return new GenerateMySqlDatabaseScript();
        }
        else if (dbInfo instanceof PostgresqlDatabase) {
            return new GeneratePostgreSQLDatabaseScript();
        }
        else if (dbInfo instanceof SqlServerDatabase) {
            return new GenerateMySqlDatabaseScript();
        }
        throw new IllegalStateException("Unknown dbInfo " + dbInfo);
    }

    public static void executeScript(Connection con, String script, boolean continueOnError) throws Exception {
        LineNumberReader in = new LineNumberReader(new StringReader( script ));
        List<String> statements = new LinkedList<>();
        JdbcTestUtils.splitSqlScript(JdbcTestUtils.readScript(in), ';', statements);
        if (continueOnError){
            con.setAutoCommit(true);
        }
        else {
            con.setAutoCommit(false);
        }
        try ( Statement s = con.createStatement(); ) {
            for (String line: statements) {
                try {
                    s.addBatch(line);
                    System.out.println(line);
                    s.executeBatch();
                }
                catch(Exception x) {
                    if (!continueOnError) {
                        con.rollback();
                        throw x;
                    }
                    System.err.println(x.getMessage());
                }
            }
            con.commit();
        }
        catch(Exception x) {
            if (!continueOnError) throw x;
        }
        //System.out.println("FINISHED EXECUTING SCRIPT " + script);
    }


    public class DataSourceDef {
        private String url;
        private String user;
        private String password;
        public DataSourceDef withUrl(String url) {
            this.url = url;
            return this;
        }
        public DataSourceDef withUser(String user) {
            this.user = user;
            return this;
        }
        public DataSourceDef withPassword(String password) {
            this.password = password;
            return this;
        }
        public DataSourceDef withDriver(String driverClassName) {
            return this;
        }
        public EnvironmentDef end() {
            withDataSource(new DriverManagerDataSource(url, user, password));
            return EnvironmentDef.this;
        }
    }

    public void createSchema() throws Exception {
        try (Connection con = dataSource.getConnection();) {
            Database dbInfo = services.getDatabaseInfo(con);
            GenerateDatabaseScript genScript = getScriptGeneratorFor( dbInfo );
            for (DefinitionsSpec spec: CreateScriptOrder.order(allSpecs)) {
                executeScript( con, genScript.generateScript(spec), false );
            }
        }
    }

    public void dropSchema() throws Exception {
        try (Connection con = dataSource.getConnection();) {
            Database dbInfo = services.getDatabaseInfo(con);
            GenerateDatabaseScript genScript = getScriptGeneratorFor( dbInfo );
            for (DefinitionsSpec spec: DropScriptOrder.order(allSpecs)) {
                executeScript( con, genScript.generateDropScript(spec), true );
            }
        }

    }

    public void dropAndCreateSchema() throws Exception {
        dropSchema();
        createSchema();
    }

}
