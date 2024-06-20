package org.example.etl.model;

import java.util.List;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

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
  public ObjectInputStream<CsvMapping> streamMappings() throws BarleyDBRuntimeException {
    try {final QueryEntityInputStream in = mappings.toManyNode.stream();
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }

  public ObjectInputStream<CsvMapping> streamMappings(QueryObject<CsvMapping> query) throws BarleyDBRuntimeException  {
    try { final QueryEntityInputStream in = mappings.toManyNode.stream(query);
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }
}
