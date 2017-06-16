package scott.barleydb.api.stream;

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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.stream.ObjectGraph.NodeId;

/**
 * Converts a stream of QueryResultItem to a stream of Entities
 * @author scott
 *
 */
public class QueryEntityInputStream implements EntityInputStream {

    private static final Logger LOG = LoggerFactory.getLogger(QueryEntityDataInputStream.class);

    private final QueryEntityDataInputStream in;
    private final EntityContext ctx;
    private final boolean generateNewCtxs;

    public QueryEntityInputStream(QueryEntityDataInputStream in, EntityContext ctx, boolean generateNewCtxs) {
        this.in = in;
        this.ctx = ctx;
        this.generateNewCtxs = generateNewCtxs;
    }

    @Override
    public Entity read() throws EntityStreamException {
        QueryResultItem queryItem = in.read();
        if (queryItem == null) {
            return null;
        }

        EntityContext ctx = getEntityContext( queryItem );

        return process( queryItem, ctx );
    }

    protected EntityContext getEntityContext(QueryResultItem queryItem) {
        return generateNewCtxs ? ctx.newEntityContextSharingTransaction() : ctx;
    }

    public void close() throws EntityStreamException {
        in.close();
    }

    private Entity process(QueryResultItem queryItem, EntityContext ctx) {
        EntityContextState prev = ctx.switchToInternalMode();
        try {
            LOG.debug("Consuming QueryEntityDataInputStream and generating a QueryResult...");
            Definitions defs = ctx.getDefinitions();
            LOG.debug("START PROCESSING QUERY RESULT ITEM FROM STEAM.");
            List<Entity> entities = new LinkedList<>();
            Entity rootEntity = null;
            for (EntityData entityData:  queryItem.getObjectGraph().getEntityData()) {
                Entity e = ctx.addEntityLoadedFromDB( entityData  );
                entities.add( e );
                if (rootEntity == null) {
                    rootEntity = e;
                }
            }
            for (NodeId nodeId: queryItem.getObjectGraph().getFetchedToManyNodes()) {
                EntityType entityType = defs.getEntityTypeMatchingInterface( nodeId.getEntityType(), true);
                Entity entity = ctx.getEntity(entityType, nodeId.getEntityKey(), true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).setFetched(true);
                entity.getChild(nodeId.getNodeName(), ToManyNode.class, true).refresh();
            }
            LOG.debug("END PROCESSING QUERY RESULT ITEM FROM STEAM.");
            return rootEntity;
        }
        finally {
            ctx.switchToMode( prev );
        }
    }


}
