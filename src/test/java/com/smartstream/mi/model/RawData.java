package com.smartstream.mi.model;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class RawData extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode data;
  private final ValueNode characterEncoding;

  public RawData(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    data = entity.getChild("data", ValueNode.class, true);
    characterEncoding = entity.getChild("characterEncoding", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public byte[] getData() {
    return data.getValue();
  }

  public void setData(byte[] data) {
    this.data.setValue(data);
  }

  public String getCharacterEncoding() {
    return characterEncoding.getValue();
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding.setValue(characterEncoding);
  }
}
