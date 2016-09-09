package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CTemplateContent;
import org.example.etl.query.QCTemplate;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCTemplateContent extends QueryObject<CTemplateContent> {
  private static final long serialVersionUID = 1L;
  public QCTemplateContent() {
    super(CTemplateContent.class);
  }

  public QCTemplateContent(QueryObject<?> parent) {
    super(CTemplateContent.class, parent);
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

  public QCTemplate joinToTemplate() {
    QCTemplate template = new QCTemplate();
    addLeftOuterJoin(template, "template");
    return template;
  }

  public QCTemplate joinToTemplate(JoinType joinType) {
    QCTemplate template = new QCTemplate();
    addJoin(template, "template", joinType);
    return template;
  }

  public QCTemplate existsTemplate() {
    QCTemplate template = new QCTemplate(this);
    addExists(template, "template");
    return template;
  }
}