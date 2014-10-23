package com.smartstream.mac.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.proxy.ProxyFactory;

public class MacProxyFactory implements ProxyFactory {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newProxy(Entity entity) {
        if (entity.getEntityType().getInterfaceName().equals(User.class.getName())) {
            return (T) new UserProxy(entity);
        }
        return null;
    }

}

class UserProxy extends AbstractCustomEntityProxy implements User {

    private static final long serialVersionUID = 1L;

    private final ValueNode id;
    private final ValueNode name;

    public UserProxy(Entity entity) {
        super(entity);
        this.id = entity.getChild("id", ValueNode.class, true);
        this.name = entity.getChild("name", ValueNode.class, true);
    }

    @Override
    public Long getId() {
        return id.getValue();
    }

    @Override
    public String getName() {
        return name.getValue();
    }

    @Override
    public void setName(String name) {
        this.name.setValue(name);
    }

}
