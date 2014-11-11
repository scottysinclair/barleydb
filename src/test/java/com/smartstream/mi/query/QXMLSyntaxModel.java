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


import scott.sort.api.query.QueryObject;

import com.smartstream.mi.model.XmlSyntaxModel;

public class QXMLSyntaxModel extends QAbstractSyntaxModel<XmlSyntaxModel, QXMLSyntaxModel> {
    private static final long serialVersionUID = 1L;

    public QXMLSyntaxModel() {
        this(null);
    }

    public QXMLSyntaxModel(QueryObject<?> parent) {
        super(XmlSyntaxModel.class, parent);
    }

    public QXMLMapping joinToMappings() {
        QXMLMapping mapping = new QXMLMapping();
        addLeftOuterJoin(mapping, "mappings");
        return mapping;
    }

    public QXMLStructure joinToStructure() {
        QXMLStructure structure = new QXMLStructure(this);
        addLeftOuterJoin(structure, "structure");
        return structure;
    }

    public QXMLMapping existsMapping() {
        QXMLMapping mapping = new QXMLMapping(this);
        addExists(mapping, "mappings");
        return mapping;
    }

    /*
    public QXMLMapping existsParentMapping() {
    	QXMLMapping mapping = new QXMLMapping(this);
    	addExists(mapping, "mappings");
    	return mapping;
    }
    */
    public QXMLStructure existsStructure() {
        QXMLStructure structure = new QXMLStructure(this);
        addExists(structure, "structure");
        return structure;
    }

}