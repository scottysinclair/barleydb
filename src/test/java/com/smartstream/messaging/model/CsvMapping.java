package com.smartstream.messaging.model;

public interface CsvMapping {

    Long getId();

    void setSyntaxModel(CsvSyntaxModel syntaxModel);

    CsvSyntaxModel getSyntaxModel();

    void setColumnIndex(Integer column);

    Integer getColumnIndex();

    void setTarget(String target);

    String getTarget();
}