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
public class XmlMapping extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper syntax;
  private final RefNodeProxyHelper subSyntax;
  private final ValueNode xpath;
  private final ValueNode targetFieldName;

  public XmlMapping(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    syntax = new RefNodeProxyHelper(entity.getChild("syntax", RefNode.class, true));
    subSyntax = new RefNodeProxyHelper(entity.getChild("subSyntax", RefNode.class, true));
    xpath = entity.getChild("xpath", ValueNode.class, true);
    targetFieldName = entity.getChild("targetFieldName", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public XmlSyntaxModel getSyntax() {
    return super.getFromRefNode(syntax.refNode);
  }

  public void setSyntax(XmlSyntaxModel syntax) {
    setToRefNode(this.syntax.refNode, syntax);
  }

  public XmlSyntaxModel getSubSyntax() {
    return super.getFromRefNode(subSyntax.refNode);
  }

  public void setSubSyntax(XmlSyntaxModel subSyntax) {
    setToRefNode(this.subSyntax.refNode, subSyntax);
  }

  public String getXpath() {
    return xpath.getValue();
  }

  public void setXpath(String xpath) {
    this.xpath.setValue(xpath);
  }

  public String getTargetFieldName() {
    return targetFieldName.getValue();
  }

  public void setTargetFieldName(String targetFieldName) {
    this.targetFieldName.setValue(targetFieldName);
  }
}
