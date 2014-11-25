package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.CsvStructureField;
import com.smartstream.mi.query.QCsvStructure;

/**
 * Generated from Entity Specification on Tue Nov 25 06:45:20 CET 2014
 *
 * @author scott
 */
public class QCsvStructureField extends QueryObject<CsvStructureField> {
  private static final long serialVersionUID = 1L;
  public QCsvStructureField() {
    super(CsvStructureField.class);
  }

  public QCsvStructureField(QueryObject<?> parent) {
    super(CsvStructureField.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QCsvStructure joinToStructure() {
    QCsvStructure structure = new QCsvStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QCsvStructure joinToStructure(JoinType joinType) {
    QCsvStructure structure = new QCsvStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QCsvStructure existsStructure() {
    QCsvStructure structure = new QCsvStructure();
    addExists(structure, "structure");
    return structure;
  }

  public QProperty<Integer> columnIndex() {
    return new QProperty<Integer>(this, "columnIndex");
  }

  public QProperty<Boolean> optional() {
    return new QProperty<Boolean>(this, "optional");
  }
}