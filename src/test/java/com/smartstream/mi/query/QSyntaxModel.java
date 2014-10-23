package com.smartstream.mi.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.mac.query.QUser;
import com.smartstream.mi.model.SyntaxModel;
import com.smartstream.mi.model.SyntaxType;

/**
 * Non parameterized query for querying base class syntax model
 */
public class QSyntaxModel extends QAbstractSyntaxModel<SyntaxModel, QSyntaxModel> {

    private static final long serialVersionUID = 1L;

    public QSyntaxModel() {
        super();
    }

    public QSyntaxModel(QueryObject<?> parent) {
        super(parent);
    }
}

/**
 * Abstract parameterized syntax query for loading some specific kind of syntax
 * @author scott
 *
 * @param <T>
 * @param <CHILD>
 */
class QAbstractSyntaxModel<T extends SyntaxModel, CHILD extends QAbstractSyntaxModel<T, CHILD>> extends QueryObject<T> {
    private static final long serialVersionUID = 1L;

    public QAbstractSyntaxModel() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public QAbstractSyntaxModel(QueryObject<?> parent) {
        super((Class<T>) SyntaxModel.class, parent);
    }

    protected QAbstractSyntaxModel(Class<T> modelClasss, QueryObject<?> parent) {
        super(modelClasss, parent);
    }

    public QUser joinToUser() {
        return joinToUser(JoinType.LEFT_OUTER);

    }
    public QUser joinToUser(JoinType joinType) {
        QUser user = new QUser();
        addJoin(user, "user", joinType);
        return user;
    }

    public QUser existsUser() {
        QUser user = new QUser(this);
        addExists(user, "user");
        return user;
    }

    public QProperty<SyntaxType> syntaxType() {
        return new QProperty<SyntaxType>(this, "syntaxType");
    }

    public QProperty<String> syntaxName() {
        return new QProperty<String>(this, "name");
    }

}