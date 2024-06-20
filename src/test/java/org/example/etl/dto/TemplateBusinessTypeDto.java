package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class TemplateBusinessTypeDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private TemplateDto template;
  private BusinessTypeDto businessType;

  public TemplateBusinessTypeDto() {
  }

  public TemplateDto getTemplate() {
    return template;
  }

  public void setTemplate(TemplateDto template) {
    this.template = template;
  }

  public BusinessTypeDto getBusinessType() {
    return businessType;
  }

  public void setBusinessType(BusinessTypeDto businessType) {
    this.businessType = businessType;
  }
  public String toString() {
    return getClass().getSimpleName() + "[]";
  }
}
