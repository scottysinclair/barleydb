package org.example.etl.dto;

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


import org.example.acl.dto.AccessAreaDto;
import org.example.etl.model.StructureType;
import org.example.etl.model.SyntaxType;
import org.example.acl.dto.UserDto;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class SyntaxModelDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private AccessAreaDto accessArea;
  private String uuid;
  private Long modifiedAt;
  private String name;
  private StructureType structureType;
  private SyntaxType syntaxType;
  private UserDto user;

  public SyntaxModelDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AccessAreaDto getAccessArea() {
    return accessArea;
  }

  public void setAccessArea(AccessAreaDto accessArea) {
    this.accessArea = accessArea;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Long getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public org.example.etl.model.StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
    this.structureType = structureType;
  }

  public org.example.etl.model.SyntaxType getSyntaxType() {
    return syntaxType;
  }

  public void setSyntaxType(org.example.etl.model.SyntaxType syntaxType) {
    this.syntaxType = syntaxType;
  }

  public UserDto getUser() {
    return user;
  }

  public void setUser(UserDto user) {
    this.user = user;
  }
}
