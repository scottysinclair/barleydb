package scott.barleydb.api.graphql;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
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

import scott.barleydb.api.core.entity.*;

import java.util.*;

public class Entity2Map {

    public static Map<String,Object> toMap(Entity entity) {
        return toMap(entity, new HashSet<Entity>());
    }

    public static Map<String,Object> toMap(Entity entity, Set<Entity> mapped) {
        if (!mapped.add(entity)) {
            return null;
        }
        Map<String,Object> result = new LinkedHashMap<>();
        for (Node node: entity.getChildren()) {
            if (node instanceof ValueNode) {
                set((ValueNode)node, result);
            }
            else if (node instanceof RefNode) {
                set((RefNode)node, result, mapped);
            }
            else if (node instanceof ToManyNode) {
                set((ToManyNode)node, result, mapped);
            }
        }
        return result;
    }

    public static void set(ValueNode node, Map<String, Object> result) {
        if (node.isLoaded()) {
            result.put(node.getName(), node.getValue());
        }
    }

    public static void set(RefNode node, Map<String, Object> result, Set<Entity> mapped) {
        if (node.isLoaded()) {
            Entity ref = node.getReference();
            if (ref != null) {
                if (!ref.isFetchRequired()) {
                    result.put(node.getName(), toMap(ref, mapped));
                }
                else {
                    //the ref exits, but the entity was not loaded so we only set the primary key
                    result.put(node.getName(), toPrimaryKey(ref));
                }
            }
        }
    }

    private static Object toPrimaryKey(Entity ref) {
        String keyNode = ref.getEntityType().getKeyNodeName();
        return ref.getChild(keyNode, ValueNode.class).getValue();
    }

    public static void set(ToManyNode node, Map<String, Object> result, Set<Entity> mapped) {
        if (node.isFetched()) {
            result.put(node.getName(), toListOfMaps(node.getList(), mapped));
        }
    }

    public static List<Map<String, Object>> toListOfMaps(List<Entity> entities) {
        return toListOfMaps(entities, new HashSet<>());
    }

    public static List<Map<String, Object>> toListOfMaps(List<Entity> entities, Set<Entity> mapped) {
        List<Map<String,Object>> list = new LinkedList<>();
        for (Entity entity: entities) {
            Map map = toMap(entity, mapped);
            if (map != null) {
              list.add(map);
            }
        }
        return list;
    }

}
