package com.smartstream.messaging.query;

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
import com.smartstream.messaging.model.CsvSyntaxModel;

import static scott.sort.api.query.JoinType.*;

public class QCsvSyntaxModel extends QueryObject<CsvSyntaxModel> {

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

    public QUser joinToUser() {
        return joinToUser(INNER);
    }
    public QUser joinToUser(JoinType joinType) {
        QUser user = new QUser();
        addJoin(user, "user", joinType);
        return user;
    }

    public QCsvStructure joinToStructure() {
        return joinToStructure(INNER);
    }
    public QCsvStructure joinToStructure(JoinType joinType) {
        QCsvStructure structure = new QCsvStructure();
        addJoin(structure, "structure",  joinType);
        return structure;
    }

    public QUser existsUser() {
        QUser user = new QUser(this);
        addExists(user, "user");
        return user;
    }

    public QCsvStructure existsStructure() {
        QCsvStructure structure = new QCsvStructure(this);
        addExists(structure, "structure");
        return structure;
    }

    public QProperty<Integer> syntaxType() {
        return new QProperty<Integer>(this, "syntaxType");
    }

    public QProperty<String> syntaxName() {
        return new QProperty<String>(this, "name");
    }

}