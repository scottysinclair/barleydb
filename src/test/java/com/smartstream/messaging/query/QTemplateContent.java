package com.smartstream.messaging.query;

import com.smartstream.messaging.model.TemplateContent;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

public class QTemplateContent extends QueryObject<TemplateContent> {

    private static final long serialVersionUID = 1L;

    public QTemplateContent() {
        super(TemplateContent.class);
    }

    public QProperty<String> name() {
        return new QProperty<String>(this, "name");
    }

    public QTemplate joinToTemplate() {
        QTemplate template = new QTemplate();
        addJoin(template, "template");
        return template;
    }

}
