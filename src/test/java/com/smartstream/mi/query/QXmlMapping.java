package com.smartstream.mi.query;

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

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.XmlMapping;
import com.smartstream.mi.query.QXmlSyntaxModel;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QXmlMapping extends QueryObject<XmlMapping> {
  private static final long serialVersionUID = 1L;
  public QXmlMapping() {
    super(XmlMapping.class);
  }

  public QXmlMapping(QueryObject<?> parent) {
    super(XmlMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QXmlSyntaxModel joinToSyntax() {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QXmlSyntaxModel joinToSyntax(JoinType joinType) {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QXmlSyntaxModel existsSyntax() {
    QXmlSyntaxModel syntax = new QXmlSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QXmlSyntaxModel joinToSubSyntax() {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel();
    addLeftOuterJoin(subSyntax, "subSyntax");
    return subSyntax;
  }

  public QXmlSyntaxModel joinToSubSyntax(JoinType joinType) {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel();
    addJoin(subSyntax, "subSyntax", joinType);
    return subSyntax;
  }

  public QXmlSyntaxModel existsSubSyntax() {
    QXmlSyntaxModel subSyntax = new QXmlSyntaxModel(this);
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