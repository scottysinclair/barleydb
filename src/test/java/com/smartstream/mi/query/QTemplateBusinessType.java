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

import scott.sort.api.query.QueryObject;

import com.smartstream.mi.model.Template;

public class QTemplateBusinessType extends QueryObject<Template> {

    private static final long serialVersionUID = 1L;

    public QTemplateBusinessType(QueryObject<?> parent) {
        super(null, "com.smartstream.mi.model.TemplateBusinessType", parent);

    }

    public QTemplateBusinessType() {
        super(null, "com.smartstream.mi.model.TemplateBusinessType", null);
    }

    public QBusinessType joinToBusinessType() {
        QBusinessType businessType = new QBusinessType();
        addLeftOuterJoin(businessType, "businessType");
        return businessType;
    }
}
