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

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.CsvStructure;

public class QCsvStructure extends QueryObject<CsvStructure> {

    private static final long serialVersionUID = 1L;

    public QCsvStructure() {
        super(CsvStructure.class);
    }

    public QCsvStructure(QueryObject<?> parent) {
        super(CsvStructure.class, parent);
    }

    public QCsvStructureField joinToFields() {
        QCsvStructureField fields = new QCsvStructureField();
        addJoin(fields, "fields");
        return fields;
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }
}
