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

import com.smartstream.messaging.model.TemplateContent;

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
        addLeftOuterJoin(template, "template");
        return template;
    }

}
