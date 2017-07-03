package org.example.etl.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
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
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.model.StructureType;
import org.example.etl.query.QXmlStructure;
import org.example.etl.query.QXmlMapping;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QXmlSyntaxModel extends QAbstractSyntaxModel<XmlSyntaxModel, QXmlSyntaxModel> {
  private static final long serialVersionUID = 1L;
  public QXmlSyntaxModel() {
    super(XmlSyntaxModel.class);
  }

  public QXmlSyntaxModel(QueryObject<?> parent) {
    super(XmlSyntaxModel.class, parent);
  }


  public QProperty<org.example.etl.model.StructureType> structureType() {
    return new QProperty<org.example.etl.model.StructureType>(this, "structureType");
  }

  public QXmlStructure joinToStructure() {
    QXmlStructure structure = new QXmlStructure();
    addLeftOuterJoin(structure, "structure");
    return structure;
  }

  public QXmlStructure joinToStructure(JoinType joinType) {
    QXmlStructure structure = new QXmlStructure();
    addJoin(structure, "structure", joinType);
    return structure;
  }

  public QXmlStructure existsStructure() {
    QXmlStructure structure = new QXmlStructure(this);
    addExists(structure, "structure");
    return structure;
  }

  public QXmlMapping joinToMappings() {
    QXmlMapping mappings = new QXmlMapping();
    addLeftOuterJoin(mappings, "mappings");
    return mappings;
  }

  public QXmlMapping joinToMappings(JoinType joinType) {
    QXmlMapping mappings = new QXmlMapping();
    addJoin(mappings, "mappings", joinType);
    return mappings;
  }

  public QXmlMapping existsMappings() {
    QXmlMapping mappings = new QXmlMapping(this);
    addExists(mappings, "mappings");
    return mappings;
  }
}
