package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


import org.example.acl.dto.AccessAreaDto;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class XmlStructureDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private AccessAreaDto accessArea;
  private String uuid;
  private Long modifiedAt;
  private String name;

  public XmlStructureDto() {
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
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
