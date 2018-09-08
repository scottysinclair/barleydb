package org.example.etl.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;

import org.example.acl.model.AccessArea;
import org.example.acl.model.User;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class SyntaxModel extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper accessArea;
  private final ValueNode uuid;
  private final ValueNode modifiedAt;
  private final ValueNode name;
  private final ValueNode structureType;
  private final ValueNode syntaxType;
  private final RefNodeProxyHelper user;

  public SyntaxModel(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    accessArea = new RefNodeProxyHelper(entity.getChild("accessArea", RefNode.class, true));
    uuid = entity.getChild("uuid", ValueNode.class, true);
    modifiedAt = entity.getChild("modifiedAt", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    structureType = entity.getChild("structureType", ValueNode.class, true);
    syntaxType = entity.getChild("syntaxType", ValueNode.class, true);
    user = new RefNodeProxyHelper(entity.getChild("user", RefNode.class, true));
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

  public org.example.etl.model.StructureType getStructureType() {
    return structureType.getValue();
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
    this.structureType.setValue(structureType);
  }

  public org.example.etl.model.SyntaxType getSyntaxType() {
    return syntaxType.getValue();
  }

  public void setSyntaxType(org.example.etl.model.SyntaxType syntaxType) {
    this.syntaxType.setValue(syntaxType);
  }

  public User getUser() {
    return super.getFromRefNode(user.refNode);
  }

  public void setUser(User user) {
    setToRefNode(this.user.refNode, user);
  }
}
