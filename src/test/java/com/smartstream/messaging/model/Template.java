package com.smartstream.messaging.model;

import java.util.List;

public interface Template {
    Long getId();

    String getName();

    void setName(String name);

    List<TemplateContent> getContents();

    List<Datatype> getDatatypes();
}
