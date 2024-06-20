package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.XmlMapping;
import org.example.etl.query.QXmlSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QXmlMapping extends QueryObject<XmlMapping> {
  private static final long serialVersionUID = 1L;
  public QXmlMapping() {
    super(XmlMapping.class);
  }

  public QXmlMapping(QueryObject<?> parent) {
    super(XmlMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<Long> syntaxId() {
    return new QProperty<Long>(this, "syntax");
  }

  public QXmlSyntaxModel joinToSyntax() {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QXmlSyntaxModel joinToSyntax(JoinType joinType) {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QXmlSyntaxModel existsSyntax() {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QProperty<Long> subSyntaxId() {
    return new QProperty<Long>(this, "subSyntax");
  }

  public QXmlSyntaxModel joinToSubSyntax() {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel();
    addLeftOuterJoin(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QXmlSyntaxModel joinToSubSyntax(JoinType joinType) {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel();
    addJoin(subSyntax, "subSyntax", joinType);
    return subSyntax;
  }

  public QXmlSyntaxModel existsSubSyntax() {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel(this);
    addExists(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QProperty<String> xpath() {
    return new QProperty<String>(this, "xpath");
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}