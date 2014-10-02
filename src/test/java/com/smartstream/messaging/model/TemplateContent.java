package com.smartstream.messaging.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public interface TemplateContent {
    Long getId();

    String getName();

    void setName(String name);

    Template getTemplate();

    void setTemplate(Template template);
}
