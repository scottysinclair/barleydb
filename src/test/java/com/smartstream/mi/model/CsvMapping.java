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

    void setSyntax(CsvSyntaxModel syntaxModel);

    CsvSyntaxModel getSyntax();

    void setStructureField(CsvStructureField field);

    CsvStructureField getStructureField();

    void setTarget(String target);

    String getTargetFieldName();
}