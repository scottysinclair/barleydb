package com.smartstream.messaging.query;

import com.smartstream.mac.query.QUser;
import com.smartstream.messaging.model.SyntaxModel;
import com.smartstream.messaging.model.SyntaxType;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

/**
 *  Non parameterized query for querying base class syntax model
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
        super((Class<T>)SyntaxModel.class, parent);
    }

    protected QAbstractSyntaxModel(Class<T> modelClasss, QueryObject<?> parent) {
        super(modelClasss, parent);
    }

    @SuppressWarnings("unchecked")
    public CHILD disableName() {
        addDisabled("name");
        return (CHILD)this;
    }

    public QUser joinToUser() {
        QUser user = new QUser();
        addJoin(user, "user");
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