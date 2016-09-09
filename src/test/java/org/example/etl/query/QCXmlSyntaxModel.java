package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CXmlSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QCXmlStructure;
import org.example.etl.query.QCXmlMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCXmlSyntaxModel extends QAbstractCSyntaxModel<CXmlSyntaxModel, QCXmlSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCXmlSyntaxModel() {
    super(CXmlSyntaxModel.class);
  }

  public QCXmlSyntaxModel(QueryObject<?> parent) {
    super(CXmlSyntaxModel.class, parent);
  }


  public QProperty<org.example.etl.model.StructureType> structureType() {
    return new QProperty<org.example.etl.model.StructureType>(this, "structureType");
  }

  public QCXmlStructure joinToStructure() {
    QCXmlStructure structure = new QCXmlStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QCXmlStructure joinToStructure(JoinType joinType) {
    QCXmlStructure structure = new QCXmlStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QCXmlStructure existsStructure() {
    QCXmlStructure structure = new QCXmlStructure(this);
    addExists(structure, "structure");
    return structure;
  }

  public QCXmlMapping joinToMappings() {
    QCXmlMapping mappings = new QCXmlMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QCXmlMapping joinToMappings(JoinType joinType) {
    QCXmlMapping mappings = new QCXmlMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QCXmlMapping existsMappings() {
    QCXmlMapping mappings = new QCXmlMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}