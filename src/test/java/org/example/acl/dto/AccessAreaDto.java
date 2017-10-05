package org.example.acl.dto;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
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

import scott.barleydb.api.dto.BaseDto;

import scott.barleydb.api.dto.DtoList;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class AccessAreaDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String name;
  private Long modifiedAt;
  private AccessAreaDto parent;
  private DtoList<AccessAreaDto> children = new DtoList<>();

  public AccessAreaDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public AccessAreaDto getParent() {
    return parent;
  }

  public void setParent(AccessAreaDto parent) {
    this.parent = parent;
  }

  public DtoList<AccessAreaDto> getChildren() {
    return children;
  }
}
