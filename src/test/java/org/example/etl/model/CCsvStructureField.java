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
public class CCsvStructureField extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final RefNodeProxyHelper structure;
  private final ValueNode columnIndex;
  private final ValueNode optional;

  public CCsvStructureField(Entity entity) {
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

  public void setId(Long id) {
    this.id.setValue(id);
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }

  public CCsvStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(CCsvStructure structure) {
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
