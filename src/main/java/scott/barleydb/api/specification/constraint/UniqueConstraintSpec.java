package scott.barleydb.api.specification.constraint;

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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class UniqueConstraintSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlAttribute
	private String name;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> nodes;
	
	public UniqueConstraintSpec() {
		this.nodes = new LinkedList<NodeSpec>();
	}

	public UniqueConstraintSpec(NodeSpec ...nodes) {
		this.nodes = new LinkedList<NodeSpec>( Arrays.asList(nodes) );
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<NodeSpec> getNodes() {
		return Collections.unmodifiableCollection( nodes );
	}
	
	public UniqueConstraintSpec newCopyFor(EntitySpec entitySpec) {
		UniqueConstraintSpec copy = new UniqueConstraintSpec();
		for (NodeSpec nodeSpec: nodes) {
			NodeSpec nodeSpecForCopy = entitySpec.getNodeSpec(nodeSpec.getName());
			if (nodeSpecForCopy == null) {
				throw new IllegalStateException("Could not find node spec in entity when creating a copy of a unique constraint");
			}
			copy.nodes.add( nodeSpecForCopy );
		}
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Unique Constraint '" + name + "' ");
		sb.append("[ ");
		for (NodeSpec spec: nodes) {
			sb.append(spec.getColumnName());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		sb.append(" ]");
		return sb.toString();
	}
	
	
}
