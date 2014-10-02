package scott.sort.api.core.proxy;

import scott.sort.api.core.entity.Entity;

public interface ProxyFactory {

    public <T> T newProxy(Entity entity);

}
