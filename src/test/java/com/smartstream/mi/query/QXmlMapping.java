package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.query.QXmlSyntaxModel;

/**
 * Generated from Entity Specification on Tue Nov 25 06:45:20 CET 2014
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
    QXmlSyntaxModel syntax = new QXmlSyntaxModel();
    addExists(syntax, "syntax");
    return syntax;
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
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel();
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