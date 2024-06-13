package scott.barleydb.api.core.entity;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.NotLoaded;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

public class ValueNode extends Node {
    private static final long serialVersionUID = 1L;
    private Object value;

    public ValueNode(Entity parent, String name) {
        super(parent, name);
        value = parent.getEntityType().getNodeType(name, true).getFixedValue();
    }

    @SuppressWarnings("unchecked")
    public <T> T getValueNoFetch() {
      return (T)value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        fetchParentIfRequiredAndAllowed();
        if (value == NotLoaded.VALUE) {
            if (getParent().getKeyValue() != null) {
                //normal fetch didn't work, we force a load of this specific property
                getEntityContext().fetch(getParent(), false, true, true, getName());
            }
            else {
                //if value == NotLoaded.VALUE it implies that a value can be loaded, therefore our entity must have
                //a PK and must exist in the database (a null value is still a value) NotLoaded means the value is undefined.
                //so we fail here to show that we are in an illogical state.
                throw new IllegalStateException("Value not loaded, but entity has no key.");
            }
        }
        return (T) value;
    }

    public boolean isLoaded() {
        return value != NotLoaded.VALUE;
    }

    public void setValueNoEvent(Object value) {
        this.value = value;
    }

    public void setValue(Object value) {
      if (getNodeType().getFixedValue() != null && !Objects.equals(value, getNodeType().getFixedValue())) {
        if (value != NotLoaded.VALUE)  {
          throw new BarleyDBRuntimeException("Entity property " + getName() + " has a fixed-value defined and cannot be changed.");
        }
      }
      Object origValue = this.value;
      setValueNoEvent(value);
      if (getParent().getKey() == this && !Objects.equals(origValue, value)) {
          getParent().handleKeySet(origValue);
      }
  }

    @Override
    public Element toXml(Document doc) {
        Element element = doc.createElement(getName());
        org.w3c.dom.Node text = doc.createTextNode(String.valueOf(value));
        element.appendChild(text);
        return element;
    }

    @Override
    public Entity getParent() {
        return (Entity) super.getParent();
    }

    public void copyFrom(ValueNode other) {
        this.value = other.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    private void fetchParentIfRequiredAndAllowed() {
        if (getParent().getKey() != this) {
            getParent().fetchIfRequiredAndAllowed();
        }
    }

}
