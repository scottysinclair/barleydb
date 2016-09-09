package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CTemplate;
import org.example.acl.query.QAccessArea;
import org.example.etl.query.QCTemplateContent;
import org.example.etl.query.QCTemplateBusinessType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCTemplate extends QueryObject<CTemplate> {
  private static final long serialVersionUID = 1L;
  public QCTemplate() {
    super(CTemplate.class);
  }

  public QCTemplate(QueryObject<?> parent) {
    super(CTemplate.class, parent);
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
    QAccessArea accessArea = new QAccessArea(this);
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

  public QCTemplateContent joinToContents() {
    QCTemplateContent contents = new QCTemplateContent();
    addLeftOuterJoin(contents, "contents");
    return contents;
  }

  public QCTemplateContent joinToContents(JoinType joinType) {
    QCTemplateContent contents = new QCTemplateContent();
    addJoin(contents, "contents", joinType);
    return contents;
  }

  public QCTemplateContent existsContents() {
    QCTemplateContent contents = new QCTemplateContent(this);
    addExists(contents, "contents");
    return contents;
  }

  public QCBusinessType joinToBusinessType() {
    QCTemplateBusinessType businessTypes = new QCTemplateBusinessType();
    addLeftOuterJoin(businessTypes, "businessTypes");
    return businessTypes.joinToBusinessType();
  }

  public QCBusinessType joinToBusinessType(JoinType joinType) {
    QCTemplateBusinessType businessTypes = new QCTemplateBusinessType();
    addJoin(businessTypes, "businessTypes", joinType);
    return businessTypes.joinToBusinessType(joinType);
  }
}