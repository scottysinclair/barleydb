package scott.barleydb.api.json;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2020 Scott Sinclair
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

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.*;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;

import java.util.List;
import java.util.Map;

public class MapToEntityConverter {

    private final EntityContext ctx;

    public MapToEntityConverter(EntityContext ctx) {
        this.ctx = ctx;
    }

    public Entity toEntity(Map<String,?> data, String tableName) {
        return toEntity(data, getEntityTypeForTable(tableName));
    }

    public Entity toEntity(Map<String,?> data, EntityType entityType) {
        if (data == null) {
            return null;
        }
        NodeType primaryKeyNodeType = entityType.getNodeType(entityType.getKeyNodeName(), true);
        Object pkValue = data.get(primaryKeyNodeType.getName());
        Entity entity;
        if (pkValue != null) {
            entity = ctx.newEntity(entityType, pkValue);
        }
        else {
            entity = ctx.newEntity(entityType);
        }

        for (Node node:  entity.getChildren()) {
            if (node.getNodeType().isPrimaryKey()) {
                continue;
            }
            else if (node instanceof ValueNode) {
                ((ValueNode) node).setValue( convertValue(node, data.get(node.getName())));
            }
            else if (node instanceof RefNode) {
                RefNode rn = (RefNode)node;
                Map<String,?> entityData = (Map<String,?>)data.get(node.getName());
                rn.setReference( toEntity(entityData, rn.getEntityType()));
            }
            else if (node instanceof ToManyNode) {
                ToManyNode tmn = (ToManyNode) node;
                List<Map<String,?>> list = (List<Map<String,?>>)data.get(node.getName());
                if (list != null) {
                    for (Map<String,?> entityData: list) {
                        Entity e = toEntity(entityData, tmn.getEntityType());
                        if (e != null) {
                            tmn.getList().add(e);
                        }
                    }
                }
            }
        }
        return entity;
    }

    protected Object convertValue(Node node, Object value) {
        return value;
    }

    private EntityType getEntityTypeForTable(String tableName) {
        for (EntityType et: ctx.getDefinitions().getEntityTypes()) {
            if (et.getTableName() != null && et.getTableName().equals(tableName)) {
                return et;
            }
        }
        return null;
    }
}
