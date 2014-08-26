package com.smartstream.messaging.model;

import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.proxy.ProxyFactory;

public class MessagingProxyFactory implements ProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T newProxy(Entity entity) {
        if (entity.getEntityType().getInterfaceName().equals(XMLSyntaxModel.class.getName())) {
            return (T)new XMLSyntaxModel(entity);
        }
        if (entity.getEntityType().getInterfaceName().equals(CsvSyntaxModel.class.getName())) {
            return (T)new CsvSyntaxModel(entity);
        }
//        if (entity.getEntityType().getInterfaceName().equals(SyntaxModel.class.getName())) {
//            return (T)new SyntaxModel(entity);
//        }
        if (entity.getEntityType().getInterfaceName().equals(XMLMapping.class.getName())) {
            return (T)new XMLMapping(entity);
        }
        return null;
    }

}



