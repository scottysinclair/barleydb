package com.smartstream.morf.server.jdbc.persister.audit;

import com.smartstream.morf.api.core.entity.Node;

public class Change {
  public final Node node; 
  public final Object oldValue;
  public final Object newValue;
  
  public Change(Node node, Object oldValue, Object newValue) {
	  this.node = node;
      this.oldValue = oldValue;
      this.newValue = newValue;
  }

  @Override
  public String toString() {
	return "Change [node=" + node + ", oldValue=" + oldValue + ", newValue="
				+ newValue + "]";
  }
  
}