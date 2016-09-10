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
import org.example.etl.model.CXmlMapping;
import org.example.etl.query.QCXmlSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCXmlMapping extends QueryObject<CXmlMapping> {
  private static final long serialVersionUID = 1L;
  public QCXmlMapping() {
    super(CXmlMapping.class);
  }

  public QCXmlMapping(QueryObject<?> parent) {
    super(CXmlMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCXmlSyntaxModel joinToSyntax() {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCXmlSyntaxModel joinToSyntax(JoinType joinType) {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCXmlSyntaxModel existsSyntax() {
    QCXmlSyntaxModel syntax = new QCXmlSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCXmlSyntaxModel joinToSubSyntax() {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel();
    addLeftOuterJoin(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QCXmlSyntaxModel joinToSubSyntax(JoinType joinType) {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel();
    addJoin(subSyntax, "subSyntax", joinType);
    return subSyntax;
  }

  public QCXmlSyntaxModel existsSubSyntax() {
    QCXmlSyntaxModel subSyntax = new QCXmlSyntaxModel(this);
    addExists(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QProperty<String> xpath() {
    return new QProperty<String>(this, "xpath");
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}