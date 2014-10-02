package com.smartstream.messaging.query;

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