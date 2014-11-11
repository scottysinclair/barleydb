package com.smartstream.mi.model;

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
import scott.sort.api.core.proxy.EntityProxy;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.exception.model.ProxyCreationException;

public class MiProxyFactory implements ProxyFactory {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public <T> T newProxy(Entity entity) throws ProxyCreationException {
        if (entity.getEntityType().getInterfaceName().equals(XmlSyntaxModel.class.getName())) {
            return (T) new XmlSyntaxModel(entity);
        }
        if (entity.getEntityType().getInterfaceName().equals(CsvSyntaxModel.class.getName())) {
            return (T) new CsvSyntaxModel(entity);
        }
//        if (entity.getEntityType().getInterfaceName().equals(SyntaxModel.class.getName())) {
//            return (T)new SyntaxModel(entity);
//        }
        if (entity.getEntityType().getInterfaceName().equals(XmlMapping.class.getName())) {
            return (T) new XmlMapping(entity);
        }
        try {
            return EntityProxy.generateProxy(getClass().getClassLoader(), entity);
        } catch (ClassNotFoundException x) {
            throw new ProxyCreationException("Could not generate dynamic proxy", x);
        }
    }

}
