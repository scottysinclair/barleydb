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
public class CTemplateContent extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final ValueNode modifiedAt;
  private final RefNodeProxyHelper template;

  public CTemplateContent(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    modifiedAt = entity.getChild("modifiedAt", ValueNode.class, true);
    template = new RefNodeProxyHelper(entity.getChild("template", RefNode.class, true));
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

  public Long getModifiedAt() {
    return modifiedAt.getValue();
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt.setValue(modifiedAt);
  }

  public CTemplate getTemplate() {
    return super.getFromRefNode(template.refNode);
  }

  public void setTemplate(CTemplate template) {
    setToRefNode(this.template.refNode, template);
  }
}
