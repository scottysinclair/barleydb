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

import com.smartstream.messaging.model.CsvStructure;

import static scott.sort.api.query.JoinType.*;

public class QCsvStructure extends QueryObject<CsvStructure> {

    private static final long serialVersionUID = 1L;

    public QCsvStructure() {
        super(CsvStructure.class);
    }

    public QCsvStructure(QueryObject<?> parent) {
        super(CsvStructure.class, parent);
    }

    public QCsvStructureField joinToFields() {
        return joinToFields(LEFT_OUTER);
    }
    public QCsvStructureField joinToFields(JoinType joinType) {
        QCsvStructureField fields = new QCsvStructureField();
        addJoin(fields, "fields", joinType);
        return fields;
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }
}
