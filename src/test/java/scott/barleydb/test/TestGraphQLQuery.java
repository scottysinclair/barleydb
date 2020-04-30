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

import org.example.etl.EtlEntityContext;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlSyntaxModel;
import org.junit.Test;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.graphql.BarleyGraphQLSchema;
import scott.barleydb.api.graphql.GraphQLContext;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.build.specification.graphql.CustomQueries;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        //executeScript("/inserts.sql", false);
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

    private int queryDepth(QJoin join) {
    	if (join == null) {
    		return 0;
    	}
    	return 2 + queryDepth(join.getFrom().getJoined());
    }
    
    
    @Test
    public void testGraphQLQuery2() throws SortServiceProviderException, SortPersistException {
    	System.out.println("-----------------------------------------------------------------------------------------");

    	EtlEntityContext ctx = new EtlEntityContext(env);
    	XmlSyntaxModel syntax = TestPersistence.buildSyntax(ctx);
    	syntax.getMappings().get(0).setSubSyntax(TestPersistence.buildSyntax(ctx));
    	syntax.getMappings().get(1).setSubSyntax(TestPersistence.buildSyntax(ctx));
    	syntax.getMappings().get(2).setSubSyntax(TestPersistence.buildSyntax(ctx));
    	ctx.persist(new PersistRequest().insert(syntax));


    	//gContext.getQueryCustomizations().setShouldBreakPredicate(new DefaultQueryBreaker(env, ctx.getNamespace(), 1, 3));
    	gContext.getQueryCustomizations().setShouldBreakPredicate((qjoin, gctx) -> {
    		return true; //qjoin.getFkeyProperty().equals("subSyntax");
//    		return true;
//    		int qDepth = queryDepth(qjoin);
  //  		return  qDepth % 4 == 0;
    	});
    	Object result = gContext.execute("{xmlSyntaxModels {" + 
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
    	                    "name \n" + 
		    	               	 "user { id \n " + 
		    	     	        "name } \n" + 
	    	               	" mappings {" + 
		        	             " id \n " + 
		        	             "targetFieldName \n" + 
		        	             " subSyntax { " + 
		        	                   "id \n" + 
		        	                    " name \n" + 
				        	               	 "user { id \n " + 
				        	     	        "name } \n" + 
			    	    	               	" mappings {" + 
					        	             " id \n " + 
					        	             "targetFieldName \n" + 
					        	             " subSyntax { " + 
					        	                   "id \n" + 
						        	              	 "user { id \n " + 
						        	     	        "name } \n" + 
					        	                    " name } } }} }} }}");

    	                   
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
