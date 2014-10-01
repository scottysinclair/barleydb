package com.smartstream.sort.api.core.proxy;

import com.smartstream.sort.api.core.entity.Entity;

public interface ProxyFactory {

    public <T> T newProxy(Entity entity);

}
