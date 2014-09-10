package com.smartstream.messaging.model;

public interface TemplateContent {
    Long getId();

    String getName();

    void setName(String name);

    Template getTemplate();

    void setTemplate(Template template);
}
