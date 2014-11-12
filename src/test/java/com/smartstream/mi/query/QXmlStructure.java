package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.XmlStructure;
import com.smartstream.mac.query.QAccessArea;

/**
 * Generated from Entity Specification on Wed Nov 12 16:58:49 CET 2014
 *
 * @author scott
 */
public class QXmlStructure extends QueryObject<XmlStructure> {
  private static final long serialVersionUID = 1L;
  public QXmlStructure() {
    super(XmlStructure.class);
  }

  public QXmlStructure(QueryObject<?> parent) {
    super(XmlStructure.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QAccessArea joinToAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addLeftOuterJoin(accessArea, "accessArea");
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
}