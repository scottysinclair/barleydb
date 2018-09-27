package org.example.etl.model;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
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
public class CsvMapping extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper syntax;
  private final RefNodeProxyHelper structureField;
  private final ValueNode targetFieldName;

  public CsvMapping(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    syntax = new RefNodeProxyHelper(entity.getChild("syntax", RefNode.class, true));
    structureField = new RefNodeProxyHelper(entity.getChild("structureField", RefNode.class, true));
    targetFieldName = entity.getChild("targetFieldName", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public CsvSyntaxModel getSyntax() {
    return super.getFromRefNode(syntax.refNode);
  }

  public void setSyntax(CsvSyntaxModel syntax) {
    setToRefNode(this.syntax.refNode, syntax);
  }

  public CsvStructureField getStructureField() {
    return super.getFromRefNode(structureField.refNode);
  }

  public void setStructureField(CsvStructureField structureField) {
    setToRefNode(this.structureField.refNode, structureField);
  }

  public String getTargetFieldName() {
    return targetFieldName.getValue();
  }

  public void setTargetFieldName(String targetFieldName) {
    this.targetFieldName.setValue(targetFieldName);
  }
}
