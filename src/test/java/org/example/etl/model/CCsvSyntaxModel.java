package org.example.etl.model;

import java.util.List;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.proxy.ToManyNodeProxyHelper;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CCsvSyntaxModel extends CSyntaxModel {
  private static final long serialVersionUID = 1L;

  private final ValueNode structureType;
  private final RefNodeProxyHelper structure;
  private final ToManyNodeProxyHelper mappings;

  public CCsvSyntaxModel(Entity entity) {
    super(entity);
    structureType = entity.getChild("structureType", ValueNode.class, true);
    structure = new RefNodeProxyHelper(entity.getChild("structure", RefNode.class, true));
    mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
  }

  public org.example.etl.model.StructureType getStructureType() {
    return structureType.getValue();
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
    this.structureType.setValue(structureType);
  }

  public CCsvStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(CCsvStructure structure) {
    setToRefNode(this.structure.refNode, structure);
  }

  public List<CCsvMapping> getMappings() {
    return super.getListProxy(mappings.toManyNode);
  }

  public void setMappings(List<CCsvMapping> mappings) {
    this.mappings.toManyNode.clear();
     for (org.example.etl.model.CCsvMapping item: mappings) {
          super.getListProxy(this.mappings.toManyNode).add( item );
     }
  }
}
