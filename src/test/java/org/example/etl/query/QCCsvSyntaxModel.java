package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CCsvSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QCCsvStructure;
import org.example.etl.query.QCCsvMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCCsvSyntaxModel extends QAbstractCSyntaxModel<CCsvSyntaxModel, QCCsvSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCCsvSyntaxModel() {
    super(CCsvSyntaxModel.class);
  }

  public QCCsvSyntaxModel(QueryObject<?> parent) {
    super(CCsvSyntaxModel.class, parent);
  }


  public QProperty<org.example.etl.model.StructureType> structureType() {
    return new QProperty<org.example.etl.model.StructureType>(this, "structureType");
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

  public QCCsvMapping joinToMappings() {
    QCCsvMapping mappings = new QCCsvMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QCCsvMapping joinToMappings(JoinType joinType) {
    QCCsvMapping mappings = new QCCsvMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QCCsvMapping existsMappings() {
    QCCsvMapping mappings = new QCCsvMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}