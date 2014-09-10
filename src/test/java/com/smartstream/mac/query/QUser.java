package com.smartstream.mac.query;

import com.smartstream.mac.model.User;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

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