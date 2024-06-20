package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class XmlMappingDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private XmlSyntaxModelDto syntax;
  private XmlSyntaxModelDto subSyntax;
  private String xpath;
  private String targetFieldName;

  public XmlMappingDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public XmlSyntaxModelDto getSyntax() {
    return syntax;
  }

  public void setSyntax(XmlSyntaxModelDto syntax) {
    this.syntax = syntax;
  }

  public XmlSyntaxModelDto getSubSyntax() {
    return subSyntax;
  }

  public void setSubSyntax(XmlSyntaxModelDto subSyntax) {
    this.subSyntax = subSyntax;
  }

  public String getXpath() {
    return xpath;
  }

  public void setXpath(String xpath) {
    this.xpath = xpath;
  }

  public String getTargetFieldName() {
    return targetFieldName;
  }

  public void setTargetFieldName(String targetFieldName) {
    this.targetFieldName = targetFieldName;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
