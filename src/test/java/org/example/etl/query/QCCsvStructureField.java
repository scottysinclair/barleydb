package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CCsvStructureField;
import org.example.etl.query.QCCsvStructure;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCCsvStructureField extends QueryObject<CCsvStructureField> {
  private static final long serialVersionUID = 1L;
  public QCCsvStructureField() {
    super(CCsvStructureField.class);
  }

  public QCCsvStructureField(QueryObject<?> parent) {
    super(CCsvStructureField.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QCCsvStructure joinToStructure() {
    QCCsvStructure structure = new QCCsvStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QCCsvStructure joinToStructure(JoinType joinType) {
    QCCsvStructure structure = new QCCsvStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QCCsvStructure existsStructure() {
    QCCsvStructure structure = new QCCsvStructure(this);
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