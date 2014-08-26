package com.smartstream.messaging.query;

import com.smartstream.messaging.model.Template;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

public class QTemplate extends QueryObject<Template> {

    private static final long serialVersionUID = 1L;

    public QTemplate() {
        super(Template.class);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }

    public QTemplateContent joinToContent() {
        QTemplateContent content = new QTemplateContent();
        addJoin(content, "contents");
        return content;
    }

    public QDatatype joinToDatatype() {
        QTemplateDatatype dt = new QTemplateDatatype();
        addJoin(dt, "datatypes");
        return dt.joinToDatatype();
    }

}
