package org.example.etl.query;

/*
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
import org.example.etl.model.CsvStructureField;
import org.example.etl.query.QCsvStructure;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCsvStructureField extends QueryObject<CsvStructureField> {
  private static final long serialVersionUID = 1L;
  public QCsvStructureField() {
    super(CsvStructureField.class);
  }

  public QCsvStructureField(QueryObject<?> parent) {
    super(CsvStructureField.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QCsvStructure joinToStructure() {
    QCsvStructure structure = new QCsvStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QCsvStructure joinToStructure(JoinType joinType) {
    QCsvStructure structure = new QCsvStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QCsvStructure existsStructure() {
    QCsvStructure structure = new QCsvStructure(this);
    addExists(structure, "structure");
    return structure;
  }

  public QProperty<Integer> columnIndex() {
    return new QProperty<Integer>(this, "columnIndex");
  }

  public QProperty<Boolean> optional() {
    return new QProperty<Boolean>(this, "optional");
  }
}