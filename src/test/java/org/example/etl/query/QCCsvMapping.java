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
import org.example.etl.model.CCsvMapping;
import org.example.etl.query.QCCsvSyntaxModel;
import org.example.etl.query.QCCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCCsvMapping extends QueryObject<CCsvMapping> {
  private static final long serialVersionUID = 1L;
  public QCCsvMapping() {
    super(CCsvMapping.class);
  }

  public QCCsvMapping(QueryObject<?> parent) {
    super(CCsvMapping.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QCCsvSyntaxModel joinToSyntax() {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel();
    addLeftOuterJoin(syntax, "syntax");
    return syntax;
  }

  public QCCsvSyntaxModel joinToSyntax(JoinType joinType) {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel();
    addJoin(syntax, "syntax", joinType);
    return syntax;
  }

  public QCCsvSyntaxModel existsSyntax() {
    QCCsvSyntaxModel syntax = new QCCsvSyntaxModel(this);
    addExists(syntax, "syntax");
    return syntax;
  }

  public QCCsvStructureField joinToStructureField() {
    QCCsvStructureField structureField = new QCCsvStructureField();
    addLeftOuterJoin(structureField, "structureField");
    return structureField;
  }

  public QCCsvStructureField joinToStructureField(JoinType joinType) {
    QCCsvStructureField structureField = new QCCsvStructureField();
    addJoin(structureField, "structureField", joinType);
    return structureField;
  }

  public QCCsvStructureField existsStructureField() {
    QCCsvStructureField structureField = new QCCsvStructureField(this);
    addExists(structureField, "structureField");
    return structureField;
  }

  public QProperty<String> targetFieldName() {
    return new QProperty<String>(this, "targetFieldName");
  }
}