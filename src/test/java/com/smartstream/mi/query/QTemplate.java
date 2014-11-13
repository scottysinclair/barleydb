package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.Template;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mi.query.QTemplateContent;
import com.smartstream.mi.query.QTemplateBusinessType;

/**
 * Generated from Entity Specification on Thu Nov 13 07:55:41 CET 2014
 *
 * @author scott
 */
public class QTemplate extends QueryObject<Template> {
  private static final long serialVersionUID = 1L;
  public QTemplate() {
    super(Template.class);
  }

  public QTemplate(QueryObject<?> parent) {
    super(Template.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QAccessArea joinToAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addLeftOuterJoin(accessArea, "accessArea");
    return accessArea;
  }

  public QAccessArea joinToAccessArea(JoinType joinType) {
    QAccessArea accessArea = new QAccessArea();
    addJoin(accessArea, "accessArea", joinType);
    return accessArea;
  }

  public QAccessArea existsAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addExists(accessArea, "accessArea");
    return accessArea;
  }

  public QProperty<String> uuid() {
    return new QProperty<String>(this, "uuid");
  }

  public QProperty<Long> modifiedAt() {
    return new QProperty<Long>(this, "modifiedAt");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QTemplateContent joinToContents() {
    QTemplateContent contents = new QTemplateContent();
    addLeftOuterJoin(contents, "contents");
    return contents;
  }

  public QTemplateContent joinToContents(JoinType joinType) {
    QTemplateContent contents = new QTemplateContent();
    addJoin(contents, "contents", joinType);
    return contents;
  }

  public QTemplateContent existsContents() {
    QTemplateContent contents = new QTemplateContent();
    addExists(contents, "contents");
    return contents;
  }

  public QBusinessType joinToBusinessType() {
    QTemplateBusinessType businessTypes = new QTemplateBusinessType();
    addLeftOuterJoin(businessTypes, "businessTypes");
    return businessTypes.joinToBusinessType();
  }

  public QBusinessType joinToBusinessType(JoinType joinType) {
    QTemplateBusinessType businessTypes = new QTemplateBusinessType();
    addJoin(businessTypes, "businessTypes", joinType);
    return businessTypes.joinToBusinessType(joinType);
  }
}