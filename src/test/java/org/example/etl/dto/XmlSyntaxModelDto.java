package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;

import scott.barleydb.api.dto.DtoList;


import org.example.etl.model.StructureType;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class XmlSyntaxModelDto extends SyntaxModelDto {
  private static final long serialVersionUID = 1L;

  private StructureType structureType;
  private XmlStructureDto structure;
  private DtoList<XmlMappingDto> mappings = new DtoList<>();

  public XmlSyntaxModelDto() {
  }

  public StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(StructureType structureType) {
    this.structureType = structureType;
  }

  public XmlStructureDto getStructure() {
    return structure;
  }

  public void setStructure(XmlStructureDto structure) {
    this.structure = structure;
  }

  public DtoList<XmlMappingDto> getMappings() {
    return mappings;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
