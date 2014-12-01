package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.CsvSyntaxModel;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.query.QCsvStructure;
import com.smartstream.mi.query.QCsvMapping;

/**
 * Generated from Entity Specification on Mon Dec 01 13:57:40 CET 2014
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
    QCsvMapping mappings = new QCsvMapping();
    addExists(mappings, "mappings");
    return mappings;
  }
}