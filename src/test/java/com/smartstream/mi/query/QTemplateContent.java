package com.smartstream.mi.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.TemplateContent;
import com.smartstream.mi.query.QTemplate;

/**
 * Generated from Entity Specification on Thu Nov 13 07:18:16 CET 2014
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
    QTemplate template = new QTemplate();
    addExists(template, "template");
    return template;
  }
}