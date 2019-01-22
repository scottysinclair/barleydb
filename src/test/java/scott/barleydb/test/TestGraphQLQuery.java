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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.example.acl.query.QUser;
import org.example.etl.model.CsvSyntaxModel;
import org.example.etl.model.SyntaxModel;
import org.example.etl.model.Template;
import org.example.etl.model.TemplateBusinessType;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QBusinessType;
import org.example.etl.query.QCsvStructure;
import org.example.etl.query.QCsvSyntaxModel;
import org.example.etl.query.QSyntaxModel;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QTemplateBusinessType;
import org.example.etl.query.QXmlMapping;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlSyntaxModel;
import org.example.etl.model.SyntaxType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.graphql.BarleyGraphQLSchema;
import scott.barleydb.api.graphql.GraphQLContext;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.DataStream;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.build.specification.graphql.CustomQueries;
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
public class TestGraphQLQuery extends TestRemoteClientBase {

    private BarleyGraphQLSchema schema; 
    private GraphQLContext gContext;

    public TestGraphQLQuery() {
        this.autoCommitMode = false;
    }

    @Override
    protected void prepareData() throws Exception {
        super.prepareData();
        executeScript("/inserts.sql", false);
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        
        CustomQueries cq = new CustomQueries();

        QXmlSyntaxModel q = new QXmlSyntaxModel();
        QXmlStructure qstruct = q.existsStructure();
        q.whereExists(qstruct.where(qstruct.id().equalsParam(new QParameter<Long>("structId", JavaType.LONG))));
        
        cq.register("syntaxWithStructureId", q);
        
    	schema = new BarleyGraphQLSchema(specRegistry, env, "org.example.etl", cq);
    	gContext = schema.newContext();
    }
    
    @Test
    public void testGraphQLQuery1() {
    	System.out.println("-----------------------------------------------------------------------------------------");
    Object result =
        gContext.execute(
            "{"
                + "\nsyn1: xmlSyntaxModelById(id: 1) { id \n name \n structureType }  "
                + "\nsyn2: xmlSyntaxModelById(id: 1) { id \n name \n structureType }  "
                + "}");
    	System.out.println(result);
    }

    @Test
    public void testGraphQLQuery2() {
    	System.out.println("-----------------------------------------------------------------------------------------");
    	Object result = gContext.execute("{xmlSyntaxModelById(id: 1) {" + 
    	" id \n " + 
    	" name \n " + 
    	"structureType \n " + 
    	 "user { id \n " + 
    	        "name } \n" + 
    	 " mappings {" + 
    	             " id \n " + 
    	             "targetFieldName \n" + 
    	             " subSyntax { " + 
    	                   "id \n" + 
    	                    " name } } }}");
    	System.out.println(result);
    }

    @Test
    public void testGraphQLQueryList() {
    	System.out.println("-----------------------------------------------------------------------------------------");
    	Map<?,List<?>> result = gContext.execute("{xmlSyntaxModels {" + 
    	" id \n " + 
    	" name \n " + 
    	"structureType \n " + 
    	 "user { id \n " + 
    	        "name } \n" + 
    	 " mappings {" + 
    	             " id \n " + 
    	             "targetFieldName \n" + 
    	             " subSyntax { " + 
    	                   "id \n" + 
    	                    " name } } }}");
    	result.values()
    	.stream()
    	.flatMap(List::stream)
    	.forEach(System.out::println);
    }

    
    @Test
    public void testGraphQLQueryListWithCondition() {
    	System.out.println("-----------------------------------------------------------------------------------------");
    	Map<?,List<?>> result = gContext.execute("{xmlSyntaxModels(name: \"syntax-xml-1\") {" +
    	" id \n " + 
    	" name \n " + 
    	"structureType \n " + 
    	 "user { id \n " + 
    	        "name } \n" + 
    	 " mappings {" + 
    	             " id \n " + 
    	             "targetFieldName \n" + 
    	             " subSyntax { " + 
    	                   "id \n" + 
    	                    " name } } }}");
    	result.values()
    	.stream()
    	.flatMap(List::stream)
    	.forEach(System.out::println);
    }

    @Test
    public void testGraphQLQueryCustomQuery() {
    	System.out.println("-----------------------------------------------------------------------------------------");
    	Map<?,List<?>> result = gContext.execute("{syntaxWithStructureId(structId: 2) {" + 
    	" id \n " + 
    	" name \n " + 
    	"structureType \n " + 
    	 "user { id \n " + 
    	        "name } \n" + 
    	 " mappings {" + 
    	             " id \n " + 
    	             "targetFieldName \n" + 
    	             " subSyntax { " + 
    	                   "id \n" + 
    	                    " name } } }}");
    	result.values()
    	.stream()
    	.flatMap(List::stream)
    	.filter(Objects::nonNull)
    	.forEach(System.out::println);
    }

  @Test
  public void testGraphQLSchemaInfo() {
    System.out.println(
        "-----------------------------------------------------------------------------------------");
    Map<?, List<?>> result =
        gContext.execute(
            "{\n"
                + "  __schema {\n"
                + "    types {\n"
                + "      name\n"
                + "    }\n"
                + "  }\n"
                + "}");

    Map<String, Object> schema = (Map<String, Object>) result.get("__schema");
    List<Map<String, Object>> types = (List<Map<String, Object>>) schema.get("types");
    for (Map<String, Object> type : types) {
      System.out.println("name => " + type.get("name"));
    }
    }
}
