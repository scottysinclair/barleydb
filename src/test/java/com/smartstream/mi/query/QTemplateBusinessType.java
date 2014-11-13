package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.TemplateBusinessType;
import com.smartstream.mi.query.QTemplate;
import com.smartstream.mi.query.QBusinessType;

/**
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
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

  public QTemplate existsTemplate() {
    QTemplate template = new QTemplate();
    addExists(template, "template");
    return template;
  }

  public QBusinessType joinToBusinessType() {
    QBusinessType businessType = new QBusinessType();
    addLeftOuterJoin(businessType, "businessType");
    return businessType;
  }

  public QBusinessType existsBusinessType() {
    QBusinessType businessType = new QBusinessType();
    addExists(businessType, "businessType");
    return businessType;
  }
}