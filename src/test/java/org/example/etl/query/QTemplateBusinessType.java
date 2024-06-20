package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.TemplateBusinessType;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QBusinessType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QTemplateBusinessType extends QueryObject<TemplateBusinessType> {
  private static final long serialVersionUID = 1L;
  public QTemplateBusinessType() {
    super(TemplateBusinessType.class);
  }

  public QTemplateBusinessType(QueryObject<?> parent) {
    super(TemplateBusinessType.class, parent);
  }


  public QProperty<Long> templateId() {
    return new QProperty<Long>(this, "template");
  }

  public QTemplate joinToTemplate() {
    QTemplate template = new QTemplate();
    addLeftOuterJoin(template, "template");
    return template;
  }

  public QTemplate joinToTemplate(JoinType joinType) {
    QTemplate template = new QTemplate();
    addJoin(template, "template", joinType);
    return template;
  }

  public QTemplate existsTemplate() {
    QTemplate template = new QTemplate(this);
    addExists(template, "template");
    return template;
  }

  public QProperty<Long> businessTypeId() {
    return new QProperty<Long>(this, "businessType");
  }

  public QBusinessType joinToBusinessType() {
    QBusinessType businessType = new QBusinessType();
    addLeftOuterJoin(businessType, "businessType");
    return businessType;
  }

  public QBusinessType joinToBusinessType(JoinType joinType) {
    QBusinessType businessType = new QBusinessType();
    addJoin(businessType, "businessType", joinType);
    return businessType;
  }

  public QBusinessType existsBusinessType() {
    QBusinessType businessType = new QBusinessType(this);
    addExists(businessType, "businessType");
    return businessType;
  }
}