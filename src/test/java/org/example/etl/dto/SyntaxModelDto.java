package org.example.etl.dto;

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

  public StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(StructureType structureType) {
    this.structureType = structureType;
  }

  public SyntaxType getSyntaxType() {
    return syntaxType;
  }

  public void setSyntaxType(SyntaxType syntaxType) {
    this.syntaxType = syntaxType;
  }

  public UserDto getUser() {
    return user;
  }

  public void setUser(UserDto user) {
    this.user = user;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
