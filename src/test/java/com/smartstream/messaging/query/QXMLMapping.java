package com.smartstream.messaging.query;

import com.smartstream.messaging.model.XMLMapping;
import com.smartstream.sort.api.query.QProperty;
import com.smartstream.sort.api.query.QueryObject;

public class QXMLMapping extends QueryObject<XMLMapping> {

    private static final long serialVersionUID = 1L;

    public QXMLMapping() {
        super(XMLMapping.class);
    }

    public QXMLMapping(QueryObject<?> parent) {
        super(XMLMapping.class, parent);
    }

    public QXMLSyntaxModel joinToSubSyntax() {
        QXMLSyntaxModel subSyntax = new QXMLSyntaxModel();
        addJoin(subSyntax, "subSyntaxModel");
        return subSyntax;
    }

    public QXMLSyntaxModel existsSubSyntax() {
        QXMLSyntaxModel subSyntax = new QXMLSyntaxModel(this);
        addExists(subSyntax, "subSyntaxModel");
        return subSyntax;
    }

    public QProperty<Long> id() {
        return new QProperty<Long>(this, "id");
    }

    public QProperty<String> xpath() {
        return new QProperty<String>(this, "xpath");
    }

}