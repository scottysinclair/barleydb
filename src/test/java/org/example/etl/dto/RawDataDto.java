package org.example.etl.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class RawDataDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private byte[] data;
  private String characterEncoding;

  public RawDataDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
