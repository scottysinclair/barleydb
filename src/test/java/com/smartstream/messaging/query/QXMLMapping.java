package com.smartstream.messaging.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.XMLMapping;

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