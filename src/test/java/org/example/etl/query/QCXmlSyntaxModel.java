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
import org.example.etl.model.CXmlSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QCXmlStructure;
import org.example.etl.query.QCXmlMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCXmlSyntaxModel extends QAbstractCSyntaxModel<CXmlSyntaxModel, QCXmlSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QCXmlSyntaxModel() {
    super(CXmlSyntaxModel.class);
  }

  public QCXmlSyntaxModel(QueryObject<?> parent) {
    super(CXmlSyntaxModel.class, parent);
  }


  public QProperty<org.example.etl.model.StructureType> structureType() {
    return new QProperty<org.example.etl.model.StructureType>(this, "structureType");
  }

  public QCXmlStructure joinToStructure() {
    QCXmlStructure structure = new QCXmlStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QCXmlStructure joinToStructure(JoinType joinType) {
    QCXmlStructure structure = new QCXmlStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QCXmlStructure existsStructure() {
    QCXmlStructure structure = new QCXmlStructure(this);
    addExists(structure, "structure");
    return structure;
  }

  public QCXmlMapping joinToMappings() {
    QCXmlMapping mappings = new QCXmlMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QCXmlMapping joinToMappings(JoinType joinType) {
    QCXmlMapping mappings = new QCXmlMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QCXmlMapping existsMappings() {
    QCXmlMapping mappings = new QCXmlMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}