package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CsvStructureFieldDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String name;
  private CsvStructureDto structure;
  private Integer columnIndex;
  private Boolean optional;

  public CsvStructureFieldDto() {
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

  public CsvStructureDto getStructure() {
    return structure;
  }

  public void setStructure(CsvStructureDto structure) {
    this.structure = structure;
  }

  public Integer getColumnIndex() {
    return columnIndex;
  }

  public void setColumnIndex(Integer columnIndex) {
    this.columnIndex = columnIndex;
  }

  public Boolean getOptional() {
    return optional;
  }

  public void setOptional(Boolean optional) {
    this.optional = optional;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
