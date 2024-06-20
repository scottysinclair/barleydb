package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;

import scott.barleydb.api.dto.DtoList;


import org.example.etl.model.StructureType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CsvSyntaxModelDto extends SyntaxModelDto {
  private static final long serialVersionUID = 1L;

  private StructureType structureType;
  private CsvStructureDto structure;
  private DtoList<CsvMappingDto> mappings = new DtoList<>();

  public CsvSyntaxModelDto() {
  }

  public StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(StructureType structureType) {
    this.structureType = structureType;
  }

  public CsvStructureDto getStructure() {
    return structure;
  }

  public void setStructure(CsvStructureDto structure) {
    this.structure = structure;
  }

  public DtoList<CsvMappingDto> getMappings() {
    return mappings;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
