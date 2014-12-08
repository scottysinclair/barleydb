package scott.sort.api.core.entity;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public enum EntityState {
    LOADING,
    NEW,
    LOADED, //the entity was loaded from the back-end
    NOTLOADED //the entity is not loaded, is a new entity if the key is null

}
