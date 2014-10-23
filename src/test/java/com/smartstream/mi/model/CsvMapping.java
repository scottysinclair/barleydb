package com.smartstream.mi.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public interface CsvMapping {

    Long getId();

    void setSyntaxModel(CsvSyntaxModel syntaxModel);

    CsvSyntaxModel getSyntaxModel();

    void setColumnIndex(Integer column);

    Integer getColumnIndex();

    void setTarget(String target);

    String getTarget();
}