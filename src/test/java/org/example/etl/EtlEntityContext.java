package org.example.etl;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.Environment;

public class EtlEntityContext extends EntityContext {

  private static final long serialVersionUID = 1L;

  public EtlEntityContext(Environment env) {
    super(env, "org.example.etl");
  }
}