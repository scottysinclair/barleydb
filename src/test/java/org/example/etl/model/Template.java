package org.example.etl.model;

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

import org.example.acl.model.AccessArea;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class Template extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper accessArea;
  private final ValueNode uuid;
  private final ValueNode modifiedAt;
  private final ValueNode name;
  private final ToManyNodeProxyHelper contents;
  private final ToManyNodeProxyHelper businessTypes;

  public Template(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    accessArea = new RefNodeProxyHelper(entity.getChild("accessArea", RefNode.class, true));
    uuid = entity.getChild("uuid", ValueNode.class, true);
    modifiedAt = entity.getChild("modifiedAt", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    contents = new ToManyNodeProxyHelper(entity.getChild("contents", ToManyNode.class, true));
    businessTypes = new ToManyNodeProxyHelper(entity.getChild("businessTypes", ToManyNode.class, true));
  }

  public Long getId() {
    return id.getValue();
  }

  public AccessArea getAccessArea() {
    return super.getFromRefNode(accessArea.refNode);
  }

  public void setAccessArea(AccessArea accessArea) {
    setToRefNode(this.accessArea.refNode, accessArea);
  }

  public String getUuid() {
    return uuid.getValue();
  }

  public void setUuid(String uuid) {
    this.uuid.setValue(uuid);
  }

  public Long getModifiedAt() {
    return modifiedAt.getValue();
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt.setValue(modifiedAt);
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }

  public List<TemplateContent> getContents() {
    return super.getListProxy(contents.toManyNode);
  }

  public List<BusinessType> getBusinessTypes() {
    return super.getListProxy(businessTypes.toManyNode);
  }
  public ObjectInputStream<TemplateContent> streamContents() throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = contents.toManyNode.stream();
    return new ObjectInputStream<>(in);
  }

  public ObjectInputStream<TemplateContent> streamContents(QueryObject<TemplateContent> query) throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = contents.toManyNode.stream(query);
    return new ObjectInputStream<>(in);
  }
  public ObjectInputStream<TemplateBusinessType> streamBusinessTypes() throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = businessTypes.toManyNode.stream();
    return new ObjectInputStream<>(in);
  }

  public ObjectInputStream<TemplateBusinessType> streamBusinessTypes(QueryObject<TemplateBusinessType> query) throws SortServiceProviderException, SortQueryException, EntityStreamException {
    final QueryEntityInputStream in = businessTypes.toManyNode.stream(query);
    return new ObjectInputStream<>(in);
  }
}
