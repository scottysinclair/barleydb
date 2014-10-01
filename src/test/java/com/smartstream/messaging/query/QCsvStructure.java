package com.smartstream.messaging.query;

import com.smartstream.messaging.model.CsvStructure;
import com.smartstream.sort.api.query.QProperty;
import com.smartstream.sort.api.query.QueryObject;

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
