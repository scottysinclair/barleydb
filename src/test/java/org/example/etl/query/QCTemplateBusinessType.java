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
import org.example.etl.model.CTemplateBusinessType;
import org.example.etl.query.QCTemplate;
import org.example.etl.query.QCBusinessType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCTemplateBusinessType extends QueryObject<CTemplateBusinessType> {
  private static final long serialVersionUID = 1L;
  public QCTemplateBusinessType() {
    super(CTemplateBusinessType.class);
  }

  public QCTemplateBusinessType(QueryObject<?> parent) {
    super(CTemplateBusinessType.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCTemplate joinToTemplate() {
    QCTemplate template = new QCTemplate();
    addLeftOuterJoin(template, "template");
    return template;
  }

  public QCTemplate joinToTemplate(JoinType joinType) {
    QCTemplate template = new QCTemplate();
    addJoin(template, "template", joinType);
    return template;
  }

  public QCTemplate existsTemplate() {
    QCTemplate template = new QCTemplate(this);
    addExists(template, "template");
    return template;
  }

  public QCBusinessType joinToBusinessType() {
    QCBusinessType businessType = new QCBusinessType();
    addLeftOuterJoin(businessType, "businessType");
    return businessType;
  }

  public QCBusinessType joinToBusinessType(JoinType joinType) {
    QCBusinessType businessType = new QCBusinessType();
    addJoin(businessType, "businessType", joinType);
    return businessType;
  }

  public QCBusinessType existsBusinessType() {
    QCBusinessType businessType = new QCBusinessType(this);
    addExists(businessType, "businessType");
    return businessType;
  }
}