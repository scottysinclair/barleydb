package scott.sort.api.core.entity;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ValueNode extends Node {
    private static final long serialVersionUID = 1L;
    private Object value;

    public ValueNode(Entity parent, String name) {
        super(parent.getEntityContext(), parent, name);
        value = parent.getEntityType().getNode(name, true).getFixedValue();
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        checkParentFetched();
        if (value == NotLoaded.VALUE) {
            if (getParent().getKey().getValue() != null) {
                //will only fetch if not in internal mode
                getEntityContext().fetch(getParent(), false, true, true);
            }
            else {
                throw new IllegalStateException("Value not loaded, but entity has no key.");
            }
        }
        return (T) value;
    }

    public void setValueNoEvent(Object value) {
        checkParentFetched();
        this.value = value;
    }

    public void setValue(Object value) {
        Object origValue = this.value;
        setValueNoEvent(value);
        if (getParent().getKey() == this && !Objects.equals(origValue, value)) {
            getParent().handleEvent(new KeySetEvent(this, origValue));
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

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    private void checkParentFetched() {
        if (getParent().getKey() != this) {
            getParent().checkFetched();
        }
    }

}
