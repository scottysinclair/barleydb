package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QXmlSyntaxModel extends QAbstractSyntaxModel<XmlSyntaxModel, QXmlSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QXmlSyntaxModel() {
    super(XmlSyntaxModel.class);
  }

  public QXmlSyntaxModel(QueryObject<?> parent) {
    super(XmlSyntaxModel.class, parent);
  }


  public QProperty<StructureType> structureType() {
    return new QProperty<StructureType>(this, "structureType");
  }

  public QProperty<Long> structureId() {
    return new QProperty<Long>(this, "structure");
  }

  public QXmlStructure joinToStructure() {
    QXmlStructure structure = new QXmlStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QXmlStructure joinToStructure(JoinType joinType) {
    QXmlStructure structure = new QXmlStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QXmlStructure existsStructure() {
    QXmlStructure structure = new QXmlStructure(this);
    addExists(structure, "structure");
    return structure;
  }

  public QXmlMapping joinToMappings() {
    QXmlMapping mappings = new QXmlMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QXmlMapping joinToMappings(JoinType joinType) {
    QXmlMapping mappings = new QXmlMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QXmlMapping existsMappings() {
    QXmlMapping mappings = new QXmlMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}