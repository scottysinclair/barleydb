package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class TemplateContentDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String name;
  private Long modifiedAt;
  private TemplateDto template;

  public TemplateContentDto() {
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

  public TemplateDto getTemplate() {
    return template;
  }

  public void setTemplate(TemplateDto template) {
    this.template = template;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
