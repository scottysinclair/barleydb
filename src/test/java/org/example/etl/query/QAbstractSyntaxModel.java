package org.example.etl.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import org.example.etl.model.SyntaxModel;
import org.example.acl.query.QAccessArea;
import org.example.etl.model.StructureType;
import org.example.etl.model.SyntaxType;
import org.example.acl.query.QUser;

/**
 * Generated from Entity Specification
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
