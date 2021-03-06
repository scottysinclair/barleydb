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

  public org.example.etl.model.StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
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
}
