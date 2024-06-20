package org.example.etl.model;

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
