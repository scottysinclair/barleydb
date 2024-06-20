package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CsvMappingDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private CsvSyntaxModelDto syntax;
  private CsvStructureFieldDto structureField;
  private String targetFieldName;

  public CsvMappingDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CsvSyntaxModelDto getSyntax() {
    return syntax;
  }

  public void setSyntax(CsvSyntaxModelDto syntax) {
    this.syntax = syntax;
  }

  public CsvStructureFieldDto getStructureField() {
    return structureField;
  }

  public void setStructureField(CsvStructureFieldDto structureField) {
    this.structureField = structureField;
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
