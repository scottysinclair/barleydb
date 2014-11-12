package com.smartstream.mac.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mac.model.AccessArea;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mac.query.QAccessArea;

/**
 * Generated from Entity Specification on Wed Nov 12 16:58:49 CET 2014
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

  public QAccessArea existsChildren() {
    QAccessArea children = new QAccessArea();
    addExists(children, "children");
    return children;
  }
}