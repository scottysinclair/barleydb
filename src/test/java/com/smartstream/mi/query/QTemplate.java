package com.smartstream.mi.query;

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

import com.smartstream.mi.model.Template;

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
        addLeftOuterJoin(content, "contents");
        return content;
    }

    public QDatatype joinToDatatype() {
        QTemplateDatatype dt = new QTemplateDatatype();
        addLeftOuterJoin(dt, "datatypes");
        return dt.joinToDatatype();
    }

}
