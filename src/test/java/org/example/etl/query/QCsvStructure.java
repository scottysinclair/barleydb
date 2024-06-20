package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CsvStructure;
import org.example.acl.query.QAccessArea;
import org.example.etl.query.QCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCsvStructure extends QueryObject<CsvStructure> {
  private static final long serialVersionUID = 1L;
  public QCsvStructure() {
    super(CsvStructure.class);
  }

  public QCsvStructure(QueryObject<?> parent) {
    super(CsvStructure.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<Long> accessAreaId() {
    return new QProperty<Long>(this, "accessArea");
  }

  public QAccessArea joinToAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addLeftOuterJoin(accessArea, "accessArea");
    return accessArea;
  }

  public QAccessArea joinToAccessArea(JoinType joinType) {
    QAccessArea accessArea = new QAccessArea();
    addJoin(accessArea, "accessArea", joinType);
    return accessArea;
  }

  public QAccessArea existsAccessArea() {
    QAccessArea accessArea = new QAccessArea(this);
    addExists(accessArea, "accessArea");
    return accessArea;
  }

  public QProperty<String> uuid() {
    return new QProperty<String>(this, "uuid");
  }

  public QProperty<Long> modifiedAt() {
    return new QProperty<Long>(this, "modifiedAt");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QProperty<Boolean> headerBasedMapping() {
    return new QProperty<Boolean>(this, "headerBasedMapping");
  }

  public QCsvStructureField joinToFields() {
    QCsvStructureField fields = new QCsvStructureField();
    addLeftOuterJoin(fields, "fields");
    return fields;
  }

  public QCsvStructureField joinToFields(JoinType joinType) {
    QCsvStructureField fields = new QCsvStructureField();
    addJoin(fields, "fields", joinType);
    return fields;
  }

  public QCsvStructureField existsFields() {
    QCsvStructureField fields = new QCsvStructureField(this);
    addExists(fields, "fields");
    return fields;
  }
}