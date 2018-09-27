package org.example.etl.model;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
public class TemplateBusinessType extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper template;
  private final RefNodeProxyHelper businessType;

  public TemplateBusinessType(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    template = new RefNodeProxyHelper(entity.getChild("template", RefNode.class, true));
    businessType = new RefNodeProxyHelper(entity.getChild("businessType", RefNode.class, true));
  }

  public Long getId() {
    return id.getValue();
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
