package scott.barleydb.test;

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

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;

public class TestEtl extends TestBase {

    @Test
    public void testEtlXmlSyntax() throws SortException {
        EntityContext ctx = new MiEntityContext(env);
        EtlTransform<XmlSyntaxModel, XmlSyntaxModel> transform = new EtlTransform<>(ctx, XmlSyntaxModel.class, XmlSyntaxModel.class);
        transform.execute(new QXmlSyntaxModel());
    }

    @Test
    public void testEtlXmlMapping() throws SortException {
        EntityContext ctx = new MiEntityContext(env);
        EtlTransform<XmlMapping, XmlMapping> transform = new EtlTransform<>(ctx, XmlMapping.class, XmlMapping.class);
        transform.execute(new QXmlMapping());
    }


    @Override
    protected void prepareData() throws Exception {
        super.prepareData();
        executeScript("/inserts.sql", false);
    }

}

class EtlTransform<SRC,DEST> {

    private final EntityContext ctx;

    public EtlTransform(EntityContext ctx, Class<SRC> class1, Class<DEST> class2) {
        this.ctx = ctx;
    }

    public void execute(QueryObject<SRC> query) throws SortException  {
        PersistRequest pr = new PersistRequest();
        QueryResult<SRC>result  = ctx.performQuery(query);
        for (Entity entity: result.getEntityList()) {
            pr.save( transform(entity) );
        }
        if (!pr.isEmpty()) {
            ctx.persist(pr);
        }
    }

    private Entity transform(Entity entity) {
        for (ValueNode vn: entity.getChildren(ValueNode.class)) {
            if (vn.getNodeType().getJavaType() == JavaType.STRING) {
                vn.setValue( vn.getValue() + "!");
            }
        }
        return entity;
    }

}