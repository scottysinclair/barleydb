package org.example.etl.query;

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
import org.example.etl.model.CTemplate;
import org.example.acl.query.QAccessArea;
import org.example.etl.query.QCTemplateContent;
import org.example.etl.query.QCTemplateBusinessType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCTemplate extends QueryObject<CTemplate> {
  private static final long serialVersionUID = 1L;
  public QCTemplate() {
    super(CTemplate.class);
  }

  public QCTemplate(QueryObject<?> parent) {
    super(CTemplate.class, parent);
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

  public QCTemplateContent joinToContents() {
    QCTemplateContent contents = new QCTemplateContent();
    addLeftOuterJoin(contents, "contents");
    return contents;
  }

  public QCTemplateContent joinToContents(JoinType joinType) {
    QCTemplateContent contents = new QCTemplateContent();
    addJoin(contents, "contents", joinType);
    return contents;
  }

  public QCTemplateContent existsContents() {
    QCTemplateContent contents = new QCTemplateContent(this);
    addExists(contents, "contents");
    return contents;
  }

  public QCBusinessType joinToBusinessType() {
    QCTemplateBusinessType businessTypes = new QCTemplateBusinessType();
    addLeftOuterJoin(businessTypes, "businessTypes");
    return businessTypes.joinToBusinessType();
  }

  public QCBusinessType joinToBusinessType(JoinType joinType) {
    QCTemplateBusinessType businessTypes = new QCTemplateBusinessType();
    addJoin(businessTypes, "businessTypes", joinType);
    return businessTypes.joinToBusinessType(joinType);
  }
}