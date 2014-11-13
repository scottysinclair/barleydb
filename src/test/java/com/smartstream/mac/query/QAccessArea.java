package com.smartstream.mac.query;

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.query.QAccessArea;

/**
 * Generated from Entity Specification on Thu Nov 13 07:18:16 CET 2014
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
    QAccessArea parent = new QAccessArea();
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
    QAccessArea children = new QAccessArea();
    addExists(children, "children");
    return children;
  }
}