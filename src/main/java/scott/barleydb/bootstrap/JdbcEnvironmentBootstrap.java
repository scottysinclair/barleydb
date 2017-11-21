package scott.barleydb.bootstrap;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.query.RuntimeProperties.Concurrency;
import scott.barleydb.api.query.RuntimeProperties.ScrollType;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.ddlgen.GenerateDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateHsqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateMySqlDatabaseScript;
import scott.barleydb.build.specification.ddlgen.GenerateOracleDatabaseScript;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;
import scott.barleydb.build.specification.staticspec.processor.StaticDefinitionProcessor;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.converter.LongToStringTimestampConverter;
import scott.barleydb.server.jdbc.persist.QuickHackSequenceGenerator;
import scott.barleydb.server.jdbc.query.QueryPreProcessor;
import scott.barleydb.server.jdbc.vendor.HsqlDatabase;
import scott.barleydb.server.jdbc.vendor.MySqlDatabase;
import scott.barleydb.server.jdbc.vendor.OracleDatabase;
import scott.barleydb.server.jdbc.vendor.PostgresqlDatabase;
import scott.barleydb.server.jdbc.vendor.SqlServerDatabase;

/**
 * Allows simple setup of the environment.
 *
 * @author scott
 *
 */
public class JdbcEnvironmentBootstrap {

    private Logger LOG = LoggerFactory.getLogger(JdbcEnvironmentBootstrap.class);

    private DataSource dataSource;
    private Concurrency concurrency = Concurrency.READ_ONLY;
    private int fetchSize = 100;
    private boolean executeInSameContext = true;
    private ScrollType scrollType = ScrollType.FORWARD_ONLY;

    private final List<String> specFiles = new LinkedList<>();

    private JdbcEntityContextServices services;
    private Environment env;

    private ClassLoader specClassLoader;
    private final List<SpecRegistry> specRegistries = new LinkedList<>();

    private String ddlGen;

    private String applicationDir = "application";

    @PostConstruct
    public void init() throws Exception {
        services = new JdbcEntityContextServices(dataSource);
        env = new Environment(services);

        /*
         * The server executes by default in the same context and provides
         * reasonable values for result-set scrolling and fetching
         */
        env.setDefaultRuntimeProperties(new RuntimeProperties().concurrency(concurrency).fetchSize(fetchSize)
                .executeInSameContext(executeInSameContext).scrollType(scrollType));

        /*
         * default preprocessor
         */
        env.setQueryPreProcessor(newQueryPreProcessor());

        services.setEnvironment(env);

        Connection connection = dataSource.getConnection();
        DatabaseMetaData metadata = connection.getMetaData();
        services.addDatabases(new HsqlDatabase(metadata), new OracleDatabase(metadata), new SqlServerDatabase(metadata),
                new MySqlDatabase(metadata), new PostgresqlDatabase(metadata));

        connection.close();

        loadDefinitions();

        services.setSequenceGenerator(new QuickHackSequenceGenerator(env));
        services.register(new LongToStringTimestampConverter());

        if (ddlGen != null)  {
            generateDDls();
        }
    }

    public void setApplicationDir(String applicationDir) {
        this.applicationDir = applicationDir;
    }

    public void setDdlGen(String ddlGen) {
        this.ddlGen = ddlGen;
    }

    public void setSpecs(List<String> specFiles) {
        this.specFiles.addAll(specFiles);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setConcurrency(Concurrency concurrency) {
        this.concurrency = concurrency;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setExecuteInSameContext(boolean executeInSameContext) {
        this.executeInSameContext = executeInSameContext;
    }

    public void setScrollType(ScrollType scrollType) {
        this.scrollType = scrollType;
    }

    public Environment getEnvironment() {
        return env;
    }

    protected QueryPreProcessor newQueryPreProcessor() {
        return new QueryPreProcessor();
    }


    private void generateDDls() throws IOException {
        File scriptsDir = new File("application/scripts");
        scriptsDir.mkdirs();
        if ("mysql".equals( ddlGen )) {
            GenerateDatabaseScript gen = new GenerateMySqlDatabaseScript();
            for (SpecRegistry reg: specRegistries) {
                for (DefinitionsSpec spec: reg.getDefinitions()) {
                    String fileName = "create-" + spec.getNamespace().replace('.', '-')  + "-mysql.sql";
                    //writeScripts(new File(scriptsDir, fileName), gen, MySqlSpecConverter.convertSpec(spec));
                    writeScripts(new File(scriptsDir, fileName), gen, spec);
                }
            }
        }
        else if ("oracle".equals( ddlGen )) {
            GenerateDatabaseScript gen = new GenerateOracleDatabaseScript();
            for (SpecRegistry reg: specRegistries) {
                for (DefinitionsSpec spec: reg.getDefinitions()) {
                    String fileName = "create-" + spec.getNamespace().replace('.', '-')  + "-oracle.sql";
                    writeScripts(new File(scriptsDir, fileName), gen, spec);
                }
            }
        }
        else if ("hsql".equals( ddlGen )) {
            GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();
            for (SpecRegistry reg: specRegistries) {
                for (DefinitionsSpec spec: reg.getDefinitions()) {
                    String fileName = "create-" + spec.getNamespace().replace('.', '-')  + "-hsql.sql";
                    writeScripts(new File(scriptsDir, fileName), gen, spec);
                }
            }
        }
        if (ddlGen != null && !"none".equals(ddlGen)) {
            GenerateDatabaseScript gen = new GenerateHsqlDatabaseScript();
            for (SpecRegistry reg: specRegistries) {
                for (DefinitionsSpec spec: reg.getDefinitions()) {
                    String fileName = "drop-" + spec.getNamespace().replace('.', '-')  + ".sql";
                    writeDropScripts(new File(scriptsDir, fileName), gen, spec);
                    fileName = "clean-" + spec.getNamespace().replace('.', '-')  + ".sql";
                    writeCleanScripts(new File(scriptsDir, fileName), gen, spec);
                }
            }
        }

    }

    private void writeScripts(File file, GenerateDatabaseScript gen, DefinitionsSpec spec) throws IOException {
        try ( Writer out = new FileWriter( file ); ) {
            out.write("/*\n Schema generated by BarleyDB static definitions\n*/\n");
            out.write(gen.generateScript( spec ) );
            out.flush();
        }
    }

    private void writeDropScripts(File file, GenerateDatabaseScript gen, DefinitionsSpec spec) throws IOException {
        try ( Writer out = new FileWriter( file ); ) {
            out.write("/*\n Schema generated by BarleyDB static definitions\n*/\n");
            out.write(gen.generateDropScript( spec ) );
            out.flush();
        }
    }

    private void writeCleanScripts(File file, GenerateDatabaseScript gen, DefinitionsSpec spec) throws IOException {
        try ( Writer out = new FileWriter( file ); ) {
            out.write("/*\n Schema generated by BarleyDB static definitions\n*/\n");
            out.write(gen.generateCleanScript( spec ) );
            out.flush();
        }
    }

    private void loadDefinitions() throws Exception {
        /*
         * if the files haven't been preconfigured then find them.
         */
        if (specFiles.isEmpty()) {
            findXmlSpecFiles();
            findSpecFilesInJars();
        }

        for (String specFile : specFiles) {
            try {
                SpecRegistry registry = loadDefinitions(specFile);
                if (registry != null) {
                    specRegistries.add(registry);
                    for (DefinitionsSpec spec : registry.getDefinitions()) {
                        env.addDefinitions(Definitions.create(spec));
                    }
                }
            }
            catch(Exception x) {
                LOG.error("Could not load spec " + specFile, x);
            }
        }
    }

    private void findSpecFilesInJars() throws Exception {
        File applicationDir = new File( this.applicationDir );

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };

        File files[] = applicationDir.listFiles(filter);
        if (files != null) {
            // look for XML files
            for (File specJar : files) {
                findSpecFilesInJar(specJar);
            }
        }
    }

    private void findSpecFilesInJar(File specJar) throws Exception {
        try (JarFile jar = new JarFile(specJar);) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                JarEntry entry = e.nextElement();
                if (entry.getName().toLowerCase().endsWith("spec.class")) {
                    specFiles.add(convertJarEntryToClassName(entry));
                }
            }
        }
    }

    private String convertJarEntryToClassName(JarEntry entry) {
        String path = entry.getName().substring(0, entry.getName().length() - 6);
        return path.replace("/", ".");
    }

    private void findXmlSpecFiles() {
        File applicationDir = new File( this.applicationDir );
        // look for XML files
        File files[] = applicationDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("spec.xml");
            }
        });
        if (files != null) {
            for (File specXml : files) {
                specFiles.add(specXml.getPath());
            }
        }
    }

    protected SpecRegistry loadDefinitions(String path) throws Exception {
        if (path.endsWith("spec.xml")) {
            JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SpecRegistry registry = (SpecRegistry) unmarshaller.unmarshal(new File(path));
            return registry;
        } else {
            ClassLoader loader = getOrCreateSpecClassLoader();
            @SuppressWarnings("unchecked")
            Class<? extends StaticDefinitions> specClass = (Class<? extends StaticDefinitions>) loader.loadClass(path);
            if (!Modifier.isAbstract( specClass.getModifiers() )) {
                SpecRegistry registry = new SpecRegistry();
                StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
                processor.process(specClass.newInstance(), registry);
                return registry;
            }
        }
        return null;
    }

    private ClassLoader getOrCreateSpecClassLoader() throws Exception {
        if (specClassLoader != null) {
            return specClassLoader;
        }
        File applicationDir = new File( this.applicationDir );
        List<URL> urls = new LinkedList<>();

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };

        for (File f : applicationDir.listFiles(filter)) {
            urls.add(f.toURI().toURL());
        }
        if (urls.isEmpty()) {
            // no jars?, perhaps the application directory contains the class
            // files directly.
            urls.add(applicationDir.toURI().toURL());
        }
        specClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        return specClassLoader;
    }

}
