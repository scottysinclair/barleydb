package org.example.mi.query;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import org.example.mac.query.QAccessArea;
import org.example.mi.model.Template;
import org.example.mi.query.QTemplateBusinessType;
import org.example.mi.query.QTemplateContent;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QTemplate extends QueryObject<Template> {
  private static final long serialVersionUID = 1L;
  public QTemplate() {
    super(Template.class);
  }

  public QTemplate(QueryObject<?> parent) {
    super(Template.class, parent);
  }


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

  public QTemplateContent joinToContents() {
    QTemplateContent contents = new QTemplateContent();
    addLeftOuterJoin(contents, "contents");
    return contents;
  }

  public QTemplateContent joinToContents(JoinType joinType) {
    QTemplateContent contents = new QTemplateContent();
    addJoin(contents, "contents", joinType);
    return contents;
  }

  public QTemplateContent existsContents() {
    QTemplateContent contents = new QTemplateContent(this);
    addExists(contents, "contents");
    return contents;
  }

  public QBusinessType joinToBusinessType() {
    QTemplateBusinessType businessTypes = new QTemplateBusinessType();
    addLeftOuterJoin(businessTypes, "businessTypes");
    return businessTypes.joinToBusinessType();
  }

  public QBusinessType joinToBusinessType(JoinType joinType) {
    QTemplateBusinessType businessTypes = new QTemplateBusinessType();
    addJoin(businessTypes, "businessTypes", joinType);
    return businessTypes.joinToBusinessType(joinType);
  }
}