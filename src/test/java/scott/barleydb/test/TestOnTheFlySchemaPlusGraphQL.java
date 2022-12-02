package scott.barleydb.test;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2022 Scott Sinclair
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

import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import schemacrawler.schema.Column;
import schemacrawler.schema.Table;
import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.graphql.BarleyGraphQLSchema;
import scott.barleydb.api.graphql.GraphQLContext;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.bootstrap.EnvironmentDef;
import scott.barleydb.build.specgen.fromdb.FromDatabaseSchemaToSpecification;
import scott.barleydb.server.jdbc.persist.QuickHackSequenceGenerator;

public class TestOnTheFlySchemaPlusGraphQL {

   private String url = "jdbc:postgresql://localhost:5432/test?currentSchema=businesscase";
   private String user = "docker_db_user";
   private String password = "docker_db_user";
   private String schema = "businesscase";

   public TestOnTheFlySchemaPlusGraphQL() {}


   @Ignore
   @Test
   public void goforit() throws Exception {
      /*
       * setup a datasource
       */
      DataSource datasource = new DriverManagerDataSource(url, user, password);

      /*
       * scan the database meta data and generate a specification
       */
      FromDatabaseSchemaToSpecification gen = new FromDatabaseSchemaToSpecification(
            "test.ontheflyy",
                      FromDatabaseSchemaToSpecification.excludeTables("flyway_schema_history"));

      SpecRegistry spec = gen.generateSpecification(datasource, schema);

      /*
       * set up an environment with the datasource and the given specification
       */
      Environment env = EnvironmentDef.build()
                                      .withDataSource(datasource)
                                      .withSequenceGenerator(QuickHackSequenceGenerator.class)
                                      .withSpecs(spec)
                                      .withNoClasses()
                                      .create();

      /*
       * use the specification and the environment to create an executable GraphQL Schema
       */
      BarleyGraphQLSchema graphQLSchema = new BarleyGraphQLSchema(spec, env, "test.ontheflyy", null);
      GraphQLContext graphql = graphQLSchema.newContext();

      /*
       * execute query
       */
      String query = """
                         {
                         caseHeads { 
                              userId
                              refType
                              case {
                                id
                                createdAt
                                caseContents {
                                  address {
                                   id
                                   street
                                   house
                                   stair
                                   door
                                   city
                                   postcode
                                  }
                                  proofOfAddress {
                                    id
                                    documentUri
                                  }
                                }
                              }
                             } 
                         }""";
      System.out.println("-----------------------------------------------------------------------------------------");
      System.out.println("Executing query");
      System.out.println(query);
      System.out.println("-----------------------------------------------------------------------------------------");
      Map<String,Object> result = graphql.execute(query);

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      System.out.println(mapper.writeValueAsString(result));


      /*
       * execute query
       */
      query = """
                         {
                         addresss(case: "320a587d-acdf-4190-aacc-e927be0ba9f2") { 
                                   id
                                   street
                                   house
                                   stair
                                   door
                                   city
                                   postcode
                                   case {
                                    id
                                   }
                             } 
                         }""";
      System.out.println("-----------------------------------------------------------------------------------------");
      System.out.println("Executing query");
      System.out.println(query);
      System.out.println("-----------------------------------------------------------------------------------------");
      result = graphql.execute(query);

      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      System.out.println(mapper.writeValueAsString(result));

   }

}
