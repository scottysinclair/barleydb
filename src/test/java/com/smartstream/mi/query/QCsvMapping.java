package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.CsvMapping;
import com.smartstream.mi.query.QCsvSyntaxModel;
import com.smartstream.mi.query.QCsvStructureField;

/**
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
 *
 * @author scott
 */
public class QCsvMapping extends QueryObject<CsvMapping> {
  private static final long serialVersionUID = 1L;
  public QCsvMapping() {
    super(CsvMapping.class);
  }

  public QCsvMapping(QueryObject<?> parent) {
    super(CsvMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCsvSyntaxModel joinToSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCsvSyntaxModel existsSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCsvStructureField joinToStructureField() {
    QCsvStructureField structureField = new QCsvStructureField();
    addLeftOuterJoin(structureField, "structureField");
    return structureField;
  }

  public QCsvStructureField existsStructureField() {
    QCsvStructureField structureField = new QCsvStructureField();
    addExists(structureField, "structureField");
    return structureField;
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}