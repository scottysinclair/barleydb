package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CsvSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QCsvStructure;
import org.example.etl.query.QCsvMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCsvSyntaxModel extends QAbstractSyntaxModel<CsvSyntaxModel, QCsvSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCsvSyntaxModel() {
    super(CsvSyntaxModel.class);
  }

  public QCsvSyntaxModel(QueryObject<?> parent) {
    super(CsvSyntaxModel.class, parent);
  }


  public QProperty<StructureType> structureType() {
    return new QProperty<StructureType>(this, "structureType");
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

  public QCsvMapping joinToMappings() {
    QCsvMapping mappings = new QCsvMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QCsvMapping joinToMappings(JoinType joinType) {
    QCsvMapping mappings = new QCsvMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QCsvMapping existsMappings() {
    QCsvMapping mappings = new QCsvMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}