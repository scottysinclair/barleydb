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

public enum EntityContextState {
    INTERNAL, //internal state - we are operating on the nodes
    USER //user - the user is accessing the nodes using the API
}
