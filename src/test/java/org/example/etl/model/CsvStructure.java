package org.example.etl.model;

import java.util.ArrayList;
import java.util.List;

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
public class CsvStructure extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper accessArea;
  private final ValueNode uuid;
  private final ValueNode modifiedAt;
  private final ValueNode name;
  private final ValueNode headerBasedMapping;
  private final ToManyNodeProxyHelper fields;

  public CsvStructure(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    accessArea = new RefNodeProxyHelper(entity.getChild("accessArea", RefNode.class, true));
    uuid = entity.getChild("uuid", ValueNode.class, true);
    modifiedAt = entity.getChild("modifiedAt", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    headerBasedMapping = entity.getChild("headerBasedMapping", ValueNode.class, true);
    fields = new ToManyNodeProxyHelper(entity.getChild("fields", ToManyNode.class, true));
  }

  public Long getId() {
    return id.getValue();
  }

  public void setId(Long id) {
    this.id.setValue(id);
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

  public Boolean getHeaderBasedMapping() {
    return headerBasedMapping.getValue();
  }

  public void setHeaderBasedMapping(Boolean headerBasedMapping) {
    this.headerBasedMapping.setValue(headerBasedMapping);
  }

  public List<CsvStructureField> getFields() {
    return super.getListProxy(fields.toManyNode);
  }

  public void setFields(List<CsvStructureField> fields) {
    List<CsvStructureField> copy = new ArrayList<>(fields);
    this.fields.toManyNode.clear();
     for (org.example.etl.model.CsvStructureField item: copy) {
          super.getListProxy(this.fields.toManyNode).add( item );
     }
  }
}
