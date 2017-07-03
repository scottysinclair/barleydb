package org.example.etl.model;

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

import java.util.List;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.SortQueryException;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.proxy.ToManyNodeProxyHelper;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class XmlSyntaxModel extends SyntaxModel {
  private static final long serialVersionUID = 1L;

  private final ValueNode structureType;
  private final RefNodeProxyHelper structure;
  private final ToManyNodeProxyHelper mappings;

  public XmlSyntaxModel(Entity entity) {
    super(entity);
    structureType = entity.getChild("structureType", ValueNode.class, true);
    structure = new RefNodeProxyHelper(entity.getChild("structure", RefNode.class, true));
    mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
  }

  public org.example.etl.model.StructureType getStructureType() {
    return structureType.getValue();
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
    this.structureType.setValue(structureType);
  }

  public XmlStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(XmlStructure structure) {
    setToRefNode(this.structure.refNode, structure);
  }

  public List<XmlMapping> getMappings() {
    return super.getListProxy(mappings.toManyNode);
  }
  public ObjectInputStream<XmlMapping> streamMappings() throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = mappings.toManyNode.stream();
    return new ObjectInputStream<>(in);
  }

  public ObjectInputStream<XmlMapping> streamMappings(QueryObject<XmlMapping> query) throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = mappings.toManyNode.stream(query);
    return new ObjectInputStream<>(in);
  }
}
