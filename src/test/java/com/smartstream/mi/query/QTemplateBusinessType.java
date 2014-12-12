package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.TemplateBusinessType;
import com.smartstream.mi.query.QTemplate;
import com.smartstream.mi.query.QBusinessType;

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


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
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