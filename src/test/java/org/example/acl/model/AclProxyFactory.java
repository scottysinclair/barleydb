package org.example.acl.model;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.exception.model.ProxyCreationException;

public class AclProxyFactory implements ProxyFactory {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public <T> T newProxy(Entity entity) throws ProxyCreationException {
    if (entity.getEntityType().getInterfaceName().equals(User.class.getName())) {
      return (T) new User(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(AccessArea.class.getName())) {
      return (T) new AccessArea(entity);
    }
    return null;
  }
}
