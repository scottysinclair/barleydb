package com.smartstream.morf.server.jdbc.persister.audit;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.Node;
import com.smartstream.morf.api.core.entity.ValueNode;

public final class AuditRecord {
  private final EntityType entityType;
  private final Object entityKey;
  private final List<Change> changes = new LinkedList<>();
  private final Set<Node> nodesChanged;
  
  public AuditRecord(EntityType entityType, Object entityKey) {
      this.entityType = entityType;
      this.entityKey = entityKey;
      this.nodesChanged = new HashSet<>();
  }
  
  public EntityType getEntityType() {
	return entityType;
  }
	
  public Object getEntityKey() {
	return entityKey;
  }

  public void addChange(Node node, Object oldValue, Object newValue) {
	if (nodesChanged.add(node)) {
		changes.add( new Change(node, oldValue, newValue) );
	}
	else {
		throw new IllegalStateException("Already consumed change for node " + node);
	}
  }
  
  public boolean hasChanges() {
	  return !changes.isEmpty();
  }
  
  public Iterable<Change> changes() {
	 return Collections.unmodifiableList(changes);
  }

  /**
   * Sets the optimistic lock change, assumes the node is not there yet
   * @param entity
   * @param newOptimisticLock
   */
  public void setOptimisticLock(Entity entity, Long newOptimisticLock) {
	  ValueNode olNode = entity.getOptimisticLock();
	  changes.add( new Change(olNode, olNode.getValue(), newOptimisticLock) );
  }

  @Override
  public String toString() {
		return "AuditRecord [entityType=" + entityType + ", entityKey=" + entityKey
				+ ", changes=" + changes + "]";
  }
    
 
}