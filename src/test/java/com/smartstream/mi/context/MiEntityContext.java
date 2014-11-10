package com.smartstream.mi.context;

import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.EntityContext;

/**
 * The EntityContext is overriden to remove the hard coded namespace
 * @author scott
 *
 */
public class MiEntityContext extends EntityContext {

    private static final long serialVersionUID = 1L;

    public MiEntityContext(Environment env) {
        super(env, "com.smartstream.mi");
    }

}
