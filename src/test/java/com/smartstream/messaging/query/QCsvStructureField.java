package com.smartstream.messaging.query;

import com.smartstream.messaging.model.CsvStructureField;
import com.smartstream.sort.api.query.QProperty;
import com.smartstream.sort.api.query.QueryObject;

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
