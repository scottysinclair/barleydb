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
import com.smartstream.mi.model.CsvMapping;
import com.smartstream.mi.query.QCsvSyntaxModel;
import com.smartstream.mi.query.QCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCsvMapping extends QueryObject<CsvMapping> {
  private static final long serialVersionUID = 1L;
  public QCsvMapping() {
    super(CsvMapping.class);
  }

  public QCsvMapping(QueryObject<?> parent) {
    super(CsvMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCsvSyntaxModel joinToSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCsvSyntaxModel joinToSyntax(JoinType joinType) {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCsvSyntaxModel existsSyntax() {
    QCsvSyntaxModel syntax = new QCsvSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCsvStructureField joinToStructureField() {
    QCsvStructureField structureField = new QCsvStructureField();
    addLeftOuterJoin(structureField, "structureField");
    return structureField;
  }

  public QCsvStructureField joinToStructureField(JoinType joinType) {
    QCsvStructureField structureField = new QCsvStructureField();
    addJoin(structureField, "structureField", joinType);
    return structureField;
  }

  public QCsvStructureField existsStructureField() {
    QCsvStructureField structureField = new QCsvStructureField(this);
    addExists(structureField, "structureField");
    return structureField;
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}