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

import java.util.List;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

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
  public ObjectInputStream<CsvStructureField> streamFields() throws BarleyDBRuntimeException {
    try {final QueryEntityInputStream in = fields.toManyNode.stream();
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }

  public ObjectInputStream<CsvStructureField> streamFields(QueryObject<CsvStructureField> query) throws BarleyDBRuntimeException  {
    try { final QueryEntityInputStream in = fields.toManyNode.stream(query);
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }
}
