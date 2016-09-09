package scott.barleydb.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QXmlMapping;
import org.example.etl.query.QXmlSyntaxModel;
import org.junit.Test;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.types.JavaType;
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
 * @author scott
 *
 */

public class TestEtl extends TestBase {

    @Test
    public void testEtlXmlSyntax() throws SortException {
        EntityContext ctx = new MiEntityContext(env);
        EtlTransform<XmlSyntaxModel, XmlSyntaxModel> transform = new EtlTransform<>(null, ctx, XmlSyntaxModel.class, XmlSyntaxModel.class);
        transform.executeLeftToRight(new QXmlSyntaxModel());
    }

    @Test
    public void testEtlXmlMapping() throws SortException {
        EntityContext ctx = new MiEntityContext(env);
        EtlTransform<XmlMapping, XmlMapping> transform = new EtlTransform<>(null, ctx, XmlMapping.class, XmlMapping.class);
        transform.executeLeftToRight(new QXmlMapping());
    }


    @Override
    protected void prepareData() throws Exception {
        super.prepareData();
        executeScript("/inserts.sql", false);
    }

}

class EtlConfiguration {
    private final Set<EntityMapping> entityMappings = new HashSet<>();

}

class EntityMapping {
    private final EntityType left;
    private final EntityType right;
    private final List<NodeMapping> mappings = new LinkedList<>();

    public EntityMapping(EntityType left, EntityType right) {
        this.left = left;
        this.right = right;
    }
}

class NodeMapping {
    private final NodeType left;
    private final NodeType right;
    private final DataTransformation transform;

    public NodeMapping(NodeType left, NodeType right, DataTransformation transform) {
        this.left = left;
        this.right = right;
        this.transform = transform;
    }

}

interface DataTransformation {
  Object transformFromLeftToRight(Object data) throws DataTransformationException;
  Object transformFromRightToLeft(Object data) throws DataTransformationException;
}


class NoopDataTransformation implements DataTransformation {

    @Override
    public Object transformFromLeftToRight(Object data) throws DataTransformationException {
        return data;
    }

    @Override
    public Object transformFromRightToLeft(Object data) throws DataTransformationException {
        return data;
    }

}

class SortEtlException extends SortException {}
class DataTransformationException extends SortEtlException {}

class EtlTransform<LEFT,RIGHT> {

    private final EtlConfiguration configuration;
    private final EntityContext ctx;


    public EtlTransform(EtlConfiguration configuration, EntityContext ctx, Class<LEFT> class1, Class<RIGHT> class2) {
        this.configuration = configuration;
        this.ctx = ctx;
    }

    public void executeLeftToRight(QueryObject<LEFT> query) throws SortException  {
        PersistRequest pr = new PersistRequest();
        QueryResult<LEFT>result  = ctx.performQuery(query);
        for (Entity entity: result.getEntityList()) {
            pr.save( transformLeftToRight(entity) );
        }
        if (!pr.isEmpty()) {
            ctx.persist(pr);
        }
    }

    private Entity transformLeftToRight(Entity entity) {
        for (ValueNode vn: entity.getChildren(ValueNode.class)) {
            if (vn.getNodeType().getJavaType() == JavaType.STRING) {
                vn.setValue( vn.getValue() + "!");
            }
        }
        return entity;
    }

}