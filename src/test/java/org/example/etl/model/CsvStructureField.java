package org.example.etl.model;

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

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CsvStructureField extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final RefNodeProxyHelper structure;
  private final ValueNode columnIndex;
  private final ValueNode optional;

  public CsvStructureField(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    structure = new RefNodeProxyHelper(entity.getChild("structure", RefNode.class, true));
    columnIndex = entity.getChild("columnIndex", ValueNode.class, true);
    optional = entity.getChild("optional", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }

  public CsvStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(CsvStructure structure) {
    setToRefNode(this.structure.refNode, structure);
  }

  public Integer getColumnIndex() {
    return columnIndex.getValue();
  }

  public void setColumnIndex(Integer columnIndex) {
    this.columnIndex.setValue(columnIndex);
  }

  public Boolean getOptional() {
    return optional.getValue();
  }

  public void setOptional(Boolean optional) {
    this.optional.setValue(optional);
  }
}
