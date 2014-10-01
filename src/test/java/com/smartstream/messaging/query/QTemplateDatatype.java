package com.smartstream.messaging.query;

import com.smartstream.messaging.model.Template;
import com.smartstream.sort.api.query.QueryObject;

public class QTemplateDatatype extends QueryObject<Template> {

    private static final long serialVersionUID = 1L;

    public QTemplateDatatype(QueryObject<?> parent) {
        super(null, "com.smartstream.messaging.TemplateDatatype", parent);

    }

    public QTemplateDatatype() {
        super(null, "com.smartstream.messaging.TemplateDatatype", null);
    }

    public QDatatype joinToDatatype() {
        QDatatype datatype = new QDatatype();
        addJoin(datatype, "datatype");
        return datatype;
    }
}
