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
 * Generated from Entity Specification on Thu Nov 13 06:32:22 CET 2014
 *
 * @author scott
 */
public class XmlSyntaxModel extends SyntaxModel {
  private static final long serialVersionUID = 1L;

  private final ValueNode structureType;
  private final RefNodeProxyHelper structure;
  private final ToManyNodeProxyHelper mappings;

  public XmlSyntaxModel(Entity entity) {
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

  public XmlStructure getStructure() {
    return super.getFromRefNode(structure.refNode);
  }

  public void setStructure(XmlStructure structure) {
    setToRefNode(this.structure.refNode, structure);
  }

  public List<XmlMapping> getMappings() {
    return super.getListProxy(mappings.toManyNode);
  }
}
