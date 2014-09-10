package com.smartstream.mac.query;

import com.smartstream.mac.model.AccessArea;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

public class QAccessArea extends QueryObject<AccessArea> {

    private static final long serialVersionUID = 1L;

    public QAccessArea() {
        super(AccessArea.class);
    }

    public QAccessArea(QueryObject<?> parent) {
        super(AccessArea.class, parent);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }

    public QAccessArea joinToParent() {
        QAccessArea parent = new QAccessArea(this);
        addJoin(parent, "parent");
        return parent;
    }

    public QAccessArea joinToChildren() {
        QAccessArea children = new QAccessArea(this);
        addJoin(children, "children");
        return children;
    }

}