package com.smartstream.messaging.query;

import com.smartstream.messaging.model.CsvMapping;
import com.smartstream.sort.api.query.QueryObject;

public class QCsvMapping extends QueryObject<CsvMapping> {

    private static final long serialVersionUID = 1L;

    public QCsvMapping() {
        super(CsvMapping.class);
    }

    public QCsvMapping(QueryObject<?> parent) {
        super(CsvMapping.class, parent);
    }

}