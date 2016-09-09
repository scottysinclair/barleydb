package org.example.etl.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.CSyntaxModel;
import org.example.acl.query.QAccessArea;
import org.example.etl.model.StructureType;
import org.example.etl.model.SyntaxType;
import org.example.acl.query.QUser;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
class QAbstractCSyntaxModel<T extends CSyntaxModel, CHILD extends QAbstractCSyntaxModel<T, CHILD>> extends QueryObject<T>{
  private static final long serialVersionUID = 1L;
  protected QAbstractCSyntaxModel(Class<T> modelClass) {
    super(modelClass);  }

  protected QAbstractCSyntaxModel(Class<T> modelClass, QueryObject<?> parent) {
    super(modelClass, parent);  }


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

  public QProperty<org.example.etl.model.StructureType> structureType() {
    return new QProperty<org.example.etl.model.StructureType>(this, "structureType");
  }

  public QProperty<org.example.etl.model.SyntaxType> syntaxType() {
    return new QProperty<org.example.etl.model.SyntaxType>(this, "syntaxType");
  }

  public QUser joinToUser() {
    QUser user = new QUser();
    addLeftOuterJoin(user, "user");
    return user;
  }

  public QUser joinToUser(JoinType joinType) {
    QUser user = new QUser();
    addJoin(user, "user", joinType);
    return user;
  }

  public QUser existsUser() {
    QUser user = new QUser(this);
    addExists(user, "user");
    return user;
  }
}