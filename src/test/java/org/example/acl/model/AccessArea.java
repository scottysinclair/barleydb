package org.example.acl.model;

import java.util.List;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.SortQueryException;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.proxy.ToManyNodeProxyHelper;

/**
 * Generated from Entity Specification
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
  public ObjectInputStream<AccessArea> streamChildren() throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = children.toManyNode.stream();
    return new ObjectInputStream<>(in);
  }

  public ObjectInputStream<AccessArea> streamChildren(QueryObject<AccessArea> query) throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = children.toManyNode.stream(query);
    return new ObjectInputStream<>(in);
  }
}
