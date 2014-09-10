package com.smartstream.messaging.query;

import com.smartstream.messaging.model.*;
import com.smartstream.morf.api.query.QueryObject;

public class QXMLSyntaxModel extends QAbstractSyntaxModel<XMLSyntaxModel, QXMLSyntaxModel> {
    private static final long serialVersionUID = 1L;

    public QXMLSyntaxModel() {
        this(null);
    }

    public QXMLSyntaxModel(QueryObject<?> parent) {
        super(XMLSyntaxModel.class, parent);
    }

    public QXMLMapping joinToMappings() {
        QXMLMapping mapping = new QXMLMapping();
        addJoin(mapping, "mappings");
        return mapping;
    }

    public QXMLStructure joinToStructure() {
        QXMLStructure structure = new QXMLStructure(this);
        addJoin(structure, "structure");
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