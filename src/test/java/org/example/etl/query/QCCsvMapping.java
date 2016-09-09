package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CCsvMapping;
import org.example.etl.query.QCCsvSyntaxModel;
import org.example.etl.query.QCCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCCsvMapping extends QueryObject<CCsvMapping> {
  private static final long serialVersionUID = 1L;
  public QCCsvMapping() {
    super(CCsvMapping.class);
  }

  public QCCsvMapping(QueryObject<?> parent) {
    super(CCsvMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCCsvSyntaxModel joinToSyntax() {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCCsvSyntaxModel joinToSyntax(JoinType joinType) {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCCsvSyntaxModel existsSyntax() {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCCsvStructureField joinToStructureField() {
    QCCsvStructureField structureField = new QCCsvStructureField();
    addLeftOuterJoin(structureField, "structureField");
    return structureField;
  }

  public QCCsvStructureField joinToStructureField(JoinType joinType) {
    QCCsvStructureField structureField = new QCCsvStructureField();
    addJoin(structureField, "structureField", joinType);
    return structureField;
  }

  public QCCsvStructureField existsStructureField() {
    QCCsvStructureField structureField = new QCCsvStructureField(this);
    addExists(structureField, "structureField");
    return structureField;
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}