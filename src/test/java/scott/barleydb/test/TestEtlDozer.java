package scott.barleydb.test;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.dozer.BeanFactory;
import org.dozer.CustomFieldMapper;
import org.dozer.DozerBeanMapper;
import org.dozer.classmap.ClassMap;
import org.dozer.fieldmap.FieldMap;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeMappingOptions;
import org.dozer.metadata.ClassMappingMetadata;
import org.dozer.metadata.FieldMappingMetadata;
import org.example.acl.model.AccessArea;
import org.example.acl.model.User;

/*-
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

import org.example.etl.context.MiEntityContext;
import org.example.etl.model.CXmlMapping;
import org.example.etl.model.CXmlStructure;
import org.example.etl.model.CXmlSyntaxModel;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlStructure;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QCXmlSyntaxModel;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlSyntaxModel;
import org.junit.Before;
import org.junit.Test;

import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;


/**
 * requirements:
 *   - track rows whch failed when transforming
 *   - report on mappings
 *
 *
 * problems:
 *  - dozer does not handle object references properly when copying, if a reference is shared in the source object graph, then there will be multiple versions of it in the target object graph
 *  -
 *
 * @author scott
 *
 */
public class TestEtlDozer extends TestBase {

    private EntityContext ctxSource;
    private EntityContext ctxDest;
    private EtlDozerContext cfg;

    @Before
    public void setup() throws Exception {
        super.setup();
        ctxSource = new MiEntityContext(env);
        ctxDest = new MiEntityContext(env);
        cfg = new EtlDozerContext(ctxDest);

        System.out.println("==============================================================");
        System.out.println("==================  MAPPING TABLE                =============");
        System.out.println("==============================================================");
        System.out.println(cfg);
        System.out.println();
        System.out.println();

        env.getDefaultRuntimeProperties().disableOptimisticLocks(true);
    }

    @Test
    public void testEtlXmlSyntax0() throws SortException {
        /*
         * ETL a whole syntax and structure across with lazy loading during walking the source bean graph
         */
        EtlDozerExecution<XmlSyntaxModel, CXmlSyntaxModel> exec = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlSyntaxModel.class, CXmlSyntaxModel.class);

        QXmlSyntaxModel query = new QXmlSyntaxModel();

        exec.executeLeftToRight( query );
    }

    @Test
    public void testEtlXmlSyntax1() throws SortException {
        /*
         * ETL a whole syntax and structure across with no lazy loading
         */
        EtlDozerExecution<XmlSyntaxModel, CXmlSyntaxModel> exec = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlSyntaxModel.class, CXmlSyntaxModel.class);

        QXmlSyntaxModel query = new QXmlSyntaxModel();
        query.joinToAccessArea().joinToParent();
        query.joinToUser();
        query.joinToMappings().joinToSubSyntax().joinToMappings();
        query.joinToStructure();

        exec.executeLeftToRight( query );
    }

    @Test
    public void testEtlXmlSyntax2() throws SortException {
        /*
         * first ETL the structures, then the syntaxes
         */
        System.out.println("==============================================================");
        System.out.println("==================  FIRST STRUCTURES             =============");
        System.out.println("==============================================================");
        EtlDozerExecution<XmlStructure, CXmlStructure> exec1 = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlStructure.class, CXmlStructure.class);
        exec1.executeLeftToRight( new QXmlStructure() );

        System.out.println("==============================================================");
        System.out.println("==================  NOW SYNTAXES                 =============");
        System.out.println("==============================================================");
        /*
         * then ETL the sytaxes and their mappings
         */
        EtlDozerExecution<XmlSyntaxModel, CXmlSyntaxModel> exec2 = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlSyntaxModel.class, CXmlSyntaxModel.class);
        QXmlSyntaxModel query = new QXmlSyntaxModel();
        query.joinToAccessArea().joinToParent();
        query.joinToUser();
        query.joinToMappings().joinToSubSyntax().joinToMappings();
        exec2.executeLeftToRight( query );
    }

    @Test
    public void testEtlXmlSyntax3() throws SortException {
        /*
         * first ETL the structures, then the syntaxes
         */
        System.out.println("==============================================================");
        System.out.println("==================  FIRST STRUCTURES             =============");
        System.out.println("==============================================================");
        EtlDozerExecution<XmlStructure, CXmlStructure> exec1 = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlStructure.class, CXmlStructure.class);
        exec1.executeLeftToRight( new QXmlStructure() );

        System.out.println("==============================================================");
        System.out.println("==================  NOW SYNTAXES                 =============");
        System.out.println("==============================================================");
        /*
         * then ETL the sytaxes and their mappings
         */
        EtlDozerExecution<XmlSyntaxModel, CXmlSyntaxModel> exec2 = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, XmlSyntaxModel.class, CXmlSyntaxModel.class);
        QXmlSyntaxModel query = new QXmlSyntaxModel();
        query.joinToAccessArea().joinToParent();
        query.joinToUser();
        query.joinToMappings().joinToSubSyntax().joinToMappings();
        exec2.executeLeftToRight( query );

        /*
         * then sync everything back the other way
         */
        System.out.println("==============================================================");
        System.out.println("==================  BACK THE OTHER WAY           =============");
        System.out.println("==============================================================");

        ctxSource.clear();
        ctxDest.clear();
        /*
         * ETL a whole syntax and structure across with no lazy loading
         */
        EtlDozerExecution<CXmlSyntaxModel, XmlSyntaxModel> exec = new EtlDozerExecution<>(cfg, ctxSource, ctxDest, CXmlSyntaxModel.class, XmlSyntaxModel.class);

        QCXmlSyntaxModel cquery = new QCXmlSyntaxModel();
        cquery.joinToAccessArea().joinToParent();
        cquery.joinToUser();
        cquery.joinToMappings().joinToSubSyntax().joinToMappings();
        cquery.joinToStructure();

        exec.executeLeftToRight( cquery );

    }


    @Override
    protected void prepareData() throws Exception {
        super.prepareData();
        executeScript("/inserts.sql", false);
    }

}

class EtlDozerContext {

    private final DozerBeanMapper mapper;
    private final BeanMappingBuilder builder;
    private final Map<Object,Object> factoryCache = new IdentityHashMap<>();
    private final Map<Object,Object> alreadyMapped = new IdentityHashMap<>();

    public EtlDozerContext(final EntityContext ctx) {
        builder = new BeanMappingBuilder() {
              protected void configure() {
                mapping(XmlSyntaxModel.class, CXmlSyntaxModel.class, TypeMappingOptions.beanFactory("MYBF"));
                mapping(XmlStructure.class, CXmlStructure.class, TypeMappingOptions.beanFactory("MYBF"));
                mapping(AccessArea.class, AccessArea.class, TypeMappingOptions.beanFactory("MYBF"));
                mapping(User.class, User.class, TypeMappingOptions.beanFactory("MYBF"));
                mapping(XmlMapping.class, CXmlMapping.class, TypeMappingOptions.beanFactory("MYBF"));
              }
        };
        builder.build();

        mapper = new DozerBeanMapper();
        mapper.addMapping(builder);
        Map<String, BeanFactory> facs = new HashMap<String,BeanFactory>();

        /*
         * do not remap objects which have been mapped before
         */
        mapper.setCustomFieldMapper(new CustomFieldMapper() {
            @Override
            public boolean mapField(Object source, Object destination, Object sourceFieldValue, ClassMap classMap, FieldMap fieldMapping) {
                //false means, treat as normal
                return alreadyMapped.containsKey(source);
            }
        });


        /*
         * the bean factory handles references correctly
         */
        final BeanFactory bf = new BeanFactory() {
            @Override
            public Object createBean(Object source, Class<?> sourceClass, String targetBeanId) {
                try {
                    Object dest = factoryCache.get(source);
                    if (dest != null) {
                        //if we already return an existing one, then we don't need to map it again
                        //so we track here
                        alreadyMapped.put(source, dest);
                        return dest;
                    }
                    else {
                        dest = ctx.newModel(Class.forName(targetBeanId), EntityConstraint.dontFetch());
                        factoryCache.put(source, dest);
                        return dest;
                    }
                } catch (ClassNotFoundException x) {
                    throw new IllegalStateException("Could not load class " + targetBeanId);
                }
            }
        };

        facs.put("MYBF", bf);
        mapper.setFactories(facs);
    }

    public void clearReferenceCache() {
        factoryCache.clear();
        alreadyMapped.clear();
    }

    public <T> T map(Object object, Class<T> destinationClass) {
        return mapper.map(object, destinationClass);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-40s %-40s %-40s %-40s %-40s%n", "SOURCE ENTITY", "SOURCE PROPERTY", "DEST ENTITY", "DEST PROPERTY", "CUSTOM CONVERTER"));
        for (ClassMappingMetadata cmmd: mapper.getMappingMetadata().getClassMappings()) {
            //sb.append("class mapping from " + cmmd.getSourceClassName() + " to " + cmmd.getDestinationClassName() + "\n");
            for (FieldMappingMetadata fmmd: cmmd.getFieldMappings()) {
                sb.append(String.format("%-40s %-40s %-40s %-40s %-40s%n", cmmd.getSourceClass().getName().toUpperCase(), fmmd.getSourceName().toUpperCase(), cmmd.getDestinationClass().getName().toUpperCase(), fmmd.getDestinationName().toUpperCase(), fmmd.getCustomConverter() != null ? fmmd.getCustomConverter().toUpperCase() : "NONE"));
              //  sb.append("  from " + fmmd.getSourceName() + " to " + fmmd.getDestinationName() + " cnv:" + fmmd.getCustomConverter() + "\n");
            }
        }
        return sb.toString();
    }


}


class EtlDozerExecution<LEFT,RIGHT> {

    private final EtlDozerContext etlDozerContext;
    private final EntityContext ctxSource;
    private final EntityContext ctxDest;
    private final Class<RIGHT> rightClass;


    public EtlDozerExecution(EtlDozerContext etlDozerContext, EntityContext ctxSource, EntityContext ctxDest, Class<LEFT> leftClass, Class<RIGHT> rightClass) {
        this.etlDozerContext = etlDozerContext;
        this.ctxSource = ctxSource;
        this.ctxDest = ctxDest;
        this.rightClass = rightClass;
    }

    public void executeLeftToRight(QueryObject<LEFT> query) throws SortException  {
        PersistRequest pr = new PersistRequest();
        System.out.println("=============== QUERY STARTING");
        QueryResult<LEFT>result  = ctxSource.performQuery(query);
        EntityContextState state = ctxDest.beginLoading();
        try {
            System.out.println("");
            System.out.println("");
            System.out.println("=============== TRANSFORM STARTED");
            for (LEFT value: result.getList()) {
                pr.save( transformLeftToRight(value) );
            }
            System.out.println("");
            System.out.println("");
            System.out.println("=============== TRANSFORM FINISHED");
        }
        finally {
           ctxDest.setEntityContextState(state);
           ctxDest.refresh();
        }

        if (!pr.isEmpty()) {
            System.out.println("");
            System.out.println("");
            System.out.println("=============== PERIST STARTED ");
            ctxDest.persist(pr);
            System.out.println("");
            System.out.println("");
            System.out.println("=============== PERIST FINISHED");
        }
    }

    private RIGHT transformLeftToRight(LEFT left) {
        return etlDozerContext.map(left, rightClass);
    }

}
