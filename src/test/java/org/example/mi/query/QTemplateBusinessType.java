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

import org.example.mi.model.TemplateBusinessType;
import org.example.mi.query.QBusinessType;
import org.example.mi.query.QTemplate;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QTemplateBusinessType extends QueryObject<TemplateBusinessType> {
  private static final long serialVersionUID = 1L;
  public QTemplateBusinessType() {
    super(TemplateBusinessType.class);
  }

  public QTemplateBusinessType(QueryObject<?> parent) {
    super(TemplateBusinessType.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QTemplate joinToTemplate() {
    QTemplate template = new QTemplate();
    addLeftOuterJoin(template, "template");
    return template;
  }

  public QTemplate joinToTemplate(JoinType joinType) {
    QTemplate template = new QTemplate();
    addJoin(template, "template", joinType);
    return template;
  }

  public QTemplate existsTemplate() {
    QTemplate template = new QTemplate(this);
    addExists(template, "template");
    return template;
  }

  public QBusinessType joinToBusinessType() {
    QBusinessType businessType = new QBusinessType();
    addLeftOuterJoin(businessType, "businessType");
    return businessType;
  }

  public QBusinessType joinToBusinessType(JoinType joinType) {
    QBusinessType businessType = new QBusinessType();
    addJoin(businessType, "businessType", joinType);
    return businessType;
  }

  public QBusinessType existsBusinessType() {
    QBusinessType businessType = new QBusinessType(this);
    addExists(businessType, "businessType");
    return businessType;
  }
}