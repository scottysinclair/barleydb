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
    NEW, //the entity does not exist in the database
    LOADED, //the entity was loaded from the back-end
    NOTLOADED //the entity is not loaded, but exists in the back end
}
