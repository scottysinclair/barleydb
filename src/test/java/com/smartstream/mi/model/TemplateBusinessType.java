package com.smartstream.mi.model;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.proxy.RefNodeProxyHelper;




public class TemplateBusinessType extends AbstractCustomEntityProxy {

  private final ValueNode id;
  private final RefNode template;
  private final RefNode businessType;


  public TemplateBusinessType(Entity entity) {
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

  public Template getTemplate() {
    return super.getFromRefNode(template.refNode);
  }

  public void setTemplate(Template template) {
    setToRefNode(this.template.refNode, template);
  }

  public BusinessType getBusinessType() {
    return super.getFromRefNode(businessType.refNode);
  }

  public void setBusinessType(BusinessType businessType) {
    setToRefNode(this.businessType.refNode, businessType);
  }
}
