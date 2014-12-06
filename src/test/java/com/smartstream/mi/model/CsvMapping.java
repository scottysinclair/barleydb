package com.smartstream.mi.model;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.proxy.RefNodeProxyHelper;

/**
 * Generated from Entity Specification on Sat Dec 06 10:17:07 CET 2014
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
