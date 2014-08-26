package com.smartstream.morf.api.core.proxy;

import com.smartstream.morf.api.core.entity.Entity;

public interface ProxyFactory {

    public <T> T newProxy(Entity entity);

}
