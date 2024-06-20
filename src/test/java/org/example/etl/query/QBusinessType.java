package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.BusinessType;
import org.example.acl.query.QAccessArea;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QBusinessType extends QueryObject<BusinessType> {
  private static final long serialVersionUID = 1L;
  public QBusinessType() {
    super(BusinessType.class);
  }

  public QBusinessType(QueryObject<?> parent) {
    super(BusinessType.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<Long> accessAreaId() {
    return new QProperty<Long>(this, "accessArea");
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
}