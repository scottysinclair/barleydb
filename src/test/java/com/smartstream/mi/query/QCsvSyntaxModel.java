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
import scott.sort.api.query.QueryObject;

import com.smartstream.mi.model.CsvSyntaxModel;

import static scott.sort.api.query.JoinType.*;

public class QCsvSyntaxModel extends QAbstractSyntaxModel<CsvSyntaxModel, QCsvSyntaxModel> {

    private static final long serialVersionUID = 1L;

    public QCsvSyntaxModel() {
        this(null);
    }

    public QCsvSyntaxModel(QueryObject<?> parent) {
        super(CsvSyntaxModel.class, parent);
    }

    public QCsvMapping joinToMappings() {
        QCsvMapping mapping = new QCsvMapping();
        addLeftOuterJoin(mapping, "mappings");
        return mapping;
    }

    public QCsvStructure joinToStructure() {
        return joinToStructure(INNER);
    }

    public QCsvStructure joinToStructure(JoinType joinType) {
        QCsvStructure structure = new QCsvStructure();
        addJoin(structure, "structure",  joinType);
        return structure;
    }

    public QCsvStructure existsStructure() {
        QCsvStructure structure = new QCsvStructure(this);
        addExists(structure, "structure");
        return structure;
    }

}