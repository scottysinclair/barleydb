package scott.sort.api.core.proxy;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ToManyNode;

public class ToManyNodeProxyHelper {

    private List<Object> list;
    public final ToManyNode toManyNode;

    public ToManyNodeProxyHelper(ToManyNode toManyNode) {
        this.toManyNode = toManyNode;
    }

    @Override
    public String toString() {
        list = new LinkedList<Object>();
        for (Entity e : toManyNode.getList()) {
            list.add(e.getEntityContext().getProxy(e));
        }
        return toManyNode.toString();
    }

}
