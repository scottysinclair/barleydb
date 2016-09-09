package org.example.etl.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;

/**
 * Generated from Entity Specification
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

  public void setId(Long id) {
    this.id.setValue(id);
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
