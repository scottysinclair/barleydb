package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CXmlMapping;
import org.example.etl.query.QCXmlSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCXmlMapping extends QueryObject<CXmlMapping> {
  private static final long serialVersionUID = 1L;
  public QCXmlMapping() {
    super(CXmlMapping.class);
  }

  public QCXmlMapping(QueryObject<?> parent) {
    super(CXmlMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCXmlSyntaxModel joinToSyntax() {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCXmlSyntaxModel joinToSyntax(JoinType joinType) {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCXmlSyntaxModel existsSyntax() {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCXmlSyntaxModel joinToSubSyntax() {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel();
    addLeftOuterJoin(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QCXmlSyntaxModel joinToSubSyntax(JoinType joinType) {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel();
    addJoin(subSyntax, "subSyntax", joinType);
    return subSyntax;
  }

  public QCXmlSyntaxModel existsSubSyntax() {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel(this);
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