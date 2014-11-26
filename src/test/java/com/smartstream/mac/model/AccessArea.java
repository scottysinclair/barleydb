package com.smartstream.mac.model;

import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.proxy.RefNodeProxyHelper;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.proxy.ToManyNodeProxyHelper;

/**
 * Generated from Entity Specification on Tue Nov 25 22:22:13 CET 2014
 *
 * @author scott
 */
public class AccessArea extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final ValueNode modifiedAt;
  private final RefNodeProxyHelper parent;
  private final ToManyNodeProxyHelper children;

  public AccessArea(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    modifiedAt = entity.getChild("modifiedAt", ValueNode.class, true);
    parent = new RefNodeProxyHelper(entity.getChild("parent", RefNode.class, true));
    children = new ToManyNodeProxyHelper(entity.getChild("children", ToManyNode.class, true));
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

  public Long getModifiedAt() {
    return modifiedAt.getValue();
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt.setValue(modifiedAt);
  }

  public AccessArea getParent() {
    return super.getFromRefNode(parent.refNode);
  }

  public void setParent(AccessArea parent) {
    setToRefNode(this.parent.refNode, parent);
  }

  public List<AccessArea> getChildren() {
    return super.getListProxy(children.toManyNode);
  }
}
