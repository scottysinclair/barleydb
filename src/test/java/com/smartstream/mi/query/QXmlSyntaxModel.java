package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.XmlSyntaxModel;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.query.QXmlStructure;
import com.smartstream.mi.query.QXmlMapping;

/**
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
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

  public QXmlStructure joinToStructure() {
    QXmlStructure structure = new QXmlStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QXmlStructure existsStructure() {
    QXmlStructure structure = new QXmlStructure();
    addExists(structure, "structure");
    return structure;
  }

  public QXmlMapping joinToMappings() {
    QXmlMapping mappings = new QXmlMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QXmlMapping existsMappings() {
    QXmlMapping mappings = new QXmlMapping();
    addExists(mappings, "mappings");
    return mappings;
  }
}