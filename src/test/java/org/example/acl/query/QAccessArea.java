package org.example.acl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.acl.model.AccessArea;
import org.example.acl.query.QAccessArea;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QAccessArea extends QueryObject<AccessArea> {
  private static final long serialVersionUID = 1L;
  public QAccessArea() {
    super(AccessArea.class);
  }

  public QAccessArea(QueryObject<?> parent) {
    super(AccessArea.class, parent);
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

  public QAccessArea joinToParent() {
    QAccessArea parent = new QAccessArea();
    addLeftOuterJoin(parent, "parent");
    return parent;
  }

  public QAccessArea joinToParent(JoinType joinType) {
    QAccessArea parent = new QAccessArea();
    addJoin(parent, "parent", joinType);
    return parent;
  }

  public QAccessArea existsParent() {
    QAccessArea parent = new QAccessArea(this);
    addExists(parent, "parent");
    return parent;
  }

  public QAccessArea joinToChildren() {
    QAccessArea children = new QAccessArea();
    addLeftOuterJoin(children, "children");
    return children;
  }

  public QAccessArea joinToChildren(JoinType joinType) {
    QAccessArea children = new QAccessArea();
    addJoin(children, "children", joinType);
    return children;
  }

  public QAccessArea existsChildren() {
    QAccessArea children = new QAccessArea(this);
    addExists(children, "children");
    return children;
  }
}