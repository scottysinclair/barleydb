package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CsvMapping;
import org.example.etl.query.QCsvSyntaxModel;
import org.example.etl.query.QCsvStructureField;

/**
 * Generated from Entity Specification
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

  public QProperty<Long> syntaxId() {
    return new QProperty<Long>(this, "syntax");
  }

  public QCsvSyntaxModel joinToSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCsvSyntaxModel joinToSyntax(JoinType joinType) {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCsvSyntaxModel existsSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QProperty<Long> structureFieldId() {
    return new QProperty<Long>(this, "structureField");
  }

  public QCsvStructureField joinToStructureField() {
    QCsvStructureField structureField = new QCsvStructureField();
    addLeftOuterJoin(structureField, "structureField");
    return structureField;
  }

  public QCsvStructureField joinToStructureField(JoinType joinType) {
    QCsvStructureField structureField = new QCsvStructureField();
    addJoin(structureField, "structureField", joinType);
    return structureField;
  }

  public QCsvStructureField existsStructureField() {
    QCsvStructureField structureField = new QCsvStructureField(this);
    addExists(structureField, "structureField");
    return structureField;
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}