package scott.barleydb.bootstrap;

import java.io.File;

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
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

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
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.converter.LongToStringTimestampConverter;
import scott.barleydb.server.jdbc.persist.QuickHackSequenceGenerator;
import scott.barleydb.server.jdbc.query.QueryPreProcessor;
import scott.barleydb.server.jdbc.vendor.HsqlDatabase;
import scott.barleydb.server.jdbc.vendor.MySqlDatabase;
import scott.barleydb.server.jdbc.vendor.OracleDatabase;
import scott.barleydb.server.jdbc.vendor.SqlServerDatabase;

/**
 * Allows simple setup of the environment.
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

    private final List<String> specXmlFiles = new LinkedList<>();
    private String topNamespace;

    private JdbcEntityContextServices services;
    private Environment env;

    @PostConstruct
    public void init() throws Exception {
        services = new JdbcEntityContextServices(dataSource);
        env = new Environment( services );

        /*
         * The server executes by default in the same context
         * and provides reasonable values for result-set scrolling and fetching
         */
        env.setDefaultRuntimeProperties(
                new RuntimeProperties()
                    .concurrency( concurrency )
                    .fetchSize( fetchSize )
                    .executeInSameContext( executeInSameContext )
                    .scrollType( scrollType ));

        /*
         * default preprocessor
         */
        env.setQueryPreProcessor( newQueryPreProcessor() );

        services.setEnvironment(env);

        Connection connection = dataSource.getConnection();
        DatabaseMetaData metadata = connection.getMetaData();
        services.addDatabases(
                new HsqlDatabase(metadata),
                new OracleDatabase(metadata),
                new SqlServerDatabase(metadata),
                new MySqlDatabase(metadata));

        connection.close();

        services.setSequenceGenerator(new QuickHackSequenceGenerator(env, "scott.picdb"));
        services.register(new LongToStringTimestampConverter());

        for (String specXmlFile: specXmlFiles){
            DefinitionsSpec spec = loadDefinitions(specXmlFile);
            Definitions defs = Definitions.create( spec );
            env.addDefinitions( defs  );
        }
    }

    public void setSpecs(List<String> specXmlFiles) {
        this.specXmlFiles.addAll( specXmlFiles );
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

      public void setTopNamespace(String topNamespace) {
        this.topNamespace = topNamespace;
    }

    public Environment getEnvironment() {
        return env;
    }

    protected QueryPreProcessor newQueryPreProcessor() {
        return new QueryPreProcessor();
    }

    protected DefinitionsSpec loadDefinitions(String path) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        SpecRegistry registry = (SpecRegistry)unmarshaller.unmarshal(new File(path));
        DefinitionsSpec spec = registry.getDefinitionsSpec(topNamespace);
        if (spec == null) {
            throw new IllegalStateException("Could not load definitions " + topNamespace);
        }
        return spec;
    }

}
