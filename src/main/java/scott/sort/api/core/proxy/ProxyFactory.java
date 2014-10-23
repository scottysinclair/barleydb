package scott.sort.api.core.proxy;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.Serializable;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.exception.model.ProxyCreationException;

/**
 * Interface for generating type safe proxy objects for the entities.
 * @author scott
 *
 */
public interface ProxyFactory extends Serializable {

    public <T> T newProxy(Entity entity) throws ProxyCreationException;

}
