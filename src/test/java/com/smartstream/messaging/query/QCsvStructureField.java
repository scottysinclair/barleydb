package com.smartstream.messaging.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;

import com.smartstream.messaging.model.CsvStructureField;

public class QCsvStructureField extends QueryObject<CsvStructureField> {

    private static final long serialVersionUID = 1L;

    public QCsvStructureField() {
        super(CsvStructureField.class);
    }

    public QCsvStructureField(QueryObject<?> parent) {
        super(CsvStructureField.class, parent);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }
}
