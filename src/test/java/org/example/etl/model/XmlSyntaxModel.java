package org.example.etl.model;

import java.util.ArrayList;
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

  public org.example.etl.model.StructureType getStructureType() {
    return structureType.getValue();
  }

  public void setStructureType(org.example.etl.model.StructureType structureType) {
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

  public void setMappings(List<XmlMapping> mappings) {
    mappings = new ArrayList<>(mappings);
    this.mappings.toManyNode.clear();
     for (org.example.etl.model.XmlMapping item: mappings) {
          super.getListProxy(this.mappings.toManyNode).add( item );
     }
  }
}
