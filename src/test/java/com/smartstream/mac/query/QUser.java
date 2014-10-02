package com.smartstream.mac.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.mac.model.User;

public class QUser extends QueryObject<User> {

    private static final long serialVersionUID = 1L;

    public QUser() {
        super(User.class);
    }

    public QUser(QueryObject<?> parent) {
        super(User.class, parent);
    }

    public QProperty<String> userName() {
        return new QProperty<String>(this, "name");
    }
}