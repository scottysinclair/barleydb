package com.smartstream.mac;

import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.EntityContext;

/**
 * The EntityContext is overriden to remove the hard coded namespace
 * @author scott
 *
 */
public class MacEntityContext extends EntityContext {

    private static final long serialVersionUID = 1L;

    public MacEntityContext(Environment env) {
        super(env, "com.smartstream.mac");
    }

}
