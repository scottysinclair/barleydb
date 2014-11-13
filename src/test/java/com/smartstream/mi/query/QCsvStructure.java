package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.CsvStructure;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mi.query.QCsvStructureField;

/**
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
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

  public QAccessArea joinToAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addLeftOuterJoin(accessArea, "accessArea");
    return accessArea;
  }

  public QAccessArea existsAccessArea() {
    QAccessArea accessArea = new QAccessArea();
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

  public QCsvStructureField existsFields() {
    QCsvStructureField fields = new QCsvStructureField();
    addExists(fields, "fields");
    return fields;
  }
}