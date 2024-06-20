package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.TemplateContent;
import org.example.etl.query.QTemplate;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QTemplateContent extends QueryObject<TemplateContent> {
  private static final long serialVersionUID = 1L;
  public QTemplateContent() {
    super(TemplateContent.class);
  }

  public QTemplateContent(QueryObject<?> parent) {
    super(TemplateContent.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QProperty<Long> modifiedAt() {
    return new QProperty<Long>(this, "modifiedAt");
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
}