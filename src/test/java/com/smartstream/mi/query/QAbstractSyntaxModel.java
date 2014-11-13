package com.smartstream.mi.query;

import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.SyntaxModel;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mi.types.StructureType;
import com.smartstream.mi.types.SyntaxType;
import com.smartstream.mac.query.QUser;

/**
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
 *
 * @author scott
 */
class QAbstractSyntaxModel<T extends SyntaxModel, CHILD extends QAbstractSyntaxModel<T, CHILD>> extends QueryObject<T>{
  private static final long serialVersionUID = 1L;
  protected QAbstractSyntaxModel(Class<T> modelClass) {
    super(modelClass);  }

  protected QAbstractSyntaxModel(Class<T> modelClass, QueryObject<?> parent) {
    super(modelClass, parent);  }


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

  public QProperty<StructureType> structureType() {
    return new QProperty<StructureType>(this, "structureType");
  }

  public QProperty<SyntaxType> syntaxType() {
    return new QProperty<SyntaxType>(this, "syntaxType");
  }

  public QUser joinToUser() {
    QUser user = new QUser();
    addLeftOuterJoin(user, "user");
    return user;
  }

  public QUser existsUser() {
    QUser user = new QUser();
    addExists(user, "user");
    return user;
  }
}