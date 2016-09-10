package org.example.acl.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
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