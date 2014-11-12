package com.smartstream.mi.model;

import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.proxy.RefNodeProxyHelper;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.proxy.ToManyNodeProxyHelper;

import com.smartstream.mi.types.StructureType;

/**
 * Generated from Entity Specification on Wed Nov 12 13:10:30 CET 2014
 *
 * @author scott
 */
public class CsvSyntaxModel extends SyntaxModel {
  private static final long serialVersionUID = 1L;

  private final ValueNode structureType;
  private final RefNodeProxyHelper structure;
  private final ToManyNodeProxyHelper mappings;

  public CsvSyntaxModel(Entity entity) {
    super(entity);
    structureType = entity.getChild("structureType", ValueNode.class, true);
    structure = new RefNodeProxyHelper(entity.getChild("structure", RefNode.class, true));
    mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
  }

  public StructureType getStructureType() {
    return structureType.getValue();
  }

  public void setStructureType(StructureType structureType) {
    this.structureType.setValue(structureType);
  }

  public CsvStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(CsvStructure structure) {
    setToRefNode(this.structure.refNode, structure);
  }

  public List<CsvMapping> getMappings() {
    return super.getListProxy(mappings.toManyNode);
  }
}
