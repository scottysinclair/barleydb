package com.smartstream.mi.model;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;

/**
 * Generated from Entity Specification on Mon Dec 01 13:57:40 CET 2014
 *
 * @author scott
 */
public class RawData extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode data;
  private final ValueNode characterEncoding;

  public RawData(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    data = entity.getChild("data", ValueNode.class, true);
    characterEncoding = entity.getChild("characterEncoding", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public byte[] getData() {
    return data.getValue();
  }

  public void setData(byte[] data) {
    this.data.setValue(data);
  }

  public String getCharacterEncoding() {
    return characterEncoding.getValue();
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding.setValue(characterEncoding);
  }
}
