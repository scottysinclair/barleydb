package com.smartstream.morf.api.core.proxy;

import java.util.LinkedList;
import java.util.List;

import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.ToManyNode;

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
