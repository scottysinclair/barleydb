package com.smartstream.mac.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.List;

public interface AccessArea {
    Long getId();

    void setName(String name);

    String getName();

    AccessArea getParent();

    void setParent(AccessArea parent);

    List<AccessArea> getChildren();
}
