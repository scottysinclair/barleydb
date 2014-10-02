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


import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.CsvMapping;

public class QCsvMapping extends QueryObject<CsvMapping> {

    private static final long serialVersionUID = 1L;

    public QCsvMapping() {
        super(CsvMapping.class);
    }

    public QCsvMapping(QueryObject<?> parent) {
        super(CsvMapping.class, parent);
    }

}