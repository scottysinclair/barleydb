package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CsvStructureField;
import org.example.etl.query.QCsvStructure;

/**
 * Generated from Entity Specification
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

  public QProperty<Long> structureId() {
    return new QProperty<Long>(this, "structure");
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
    QCsvStructure structure = new QCsvStructure(this);
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