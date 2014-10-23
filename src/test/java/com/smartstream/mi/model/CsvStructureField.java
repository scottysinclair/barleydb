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

public interface CsvStructureField {

    Long getId();

    String getName();

    void setName(String name);

    Integer getColumnIndex();

    void setColumnIndex(Integer columnIndex);

    Boolean getOptional();

    void setOptional(Boolean optional);

    CsvStructure getStructure();

    void setStructure(CsvStructure structure);

}
