package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CCsvStructure;
import org.example.acl.query.QAccessArea;
import org.example.etl.query.QCCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCCsvStructure extends QueryObject<CCsvStructure> {
  private static final long serialVersionUID = 1L;
  public QCCsvStructure() {
    super(CCsvStructure.class);
  }

  public QCCsvStructure(QueryObject<?> parent) {
    super(CCsvStructure.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
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

  public QCCsvStructureField joinToFields() {
    QCCsvStructureField fields = new QCCsvStructureField();
    addLeftOuterJoin(fields, "fields");
    return fields;
  }

  public QCCsvStructureField joinToFields(JoinType joinType) {
    QCCsvStructureField fields = new QCCsvStructureField();
    addJoin(fields, "fields", joinType);
    return fields;
  }

  public QCCsvStructureField existsFields() {
    QCCsvStructureField fields = new QCCsvStructureField(this);
    addExists(fields, "fields");
    return fields;
  }
}