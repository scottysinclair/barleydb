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
public class CTemplateBusinessType extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper template;
  private final RefNodeProxyHelper businessType;

  public CTemplateBusinessType(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    template = new RefNodeProxyHelper(entity.getChild("template", RefNode.class, true));
    businessType = new RefNodeProxyHelper(entity.getChild("businessType", RefNode.class, true));
  }

  public Long getId() {
    return id.getValue();
  }

  public void setId(Long id) {
    this.id.setValue(id);
  }

  public CTemplate getTemplate() {
    return super.getFromRefNode(template.refNode);
  }

  public void setTemplate(CTemplate template) {
    setToRefNode(this.template.refNode, template);
  }

  public CBusinessType getBusinessType() {
    return super.getFromRefNode(businessType.refNode);
  }

  public void setBusinessType(CBusinessType businessType) {
    setToRefNode(this.businessType.refNode, businessType);
  }
}
