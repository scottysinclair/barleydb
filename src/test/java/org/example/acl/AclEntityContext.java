package org.example.acl;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.Environment;

public class AclEntityContext extends EntityContext {

  private static final long serialVersionUID = 1L;

  public AclEntityContext(Environment env) {
    super(env, "org.example.acl");
  }
}