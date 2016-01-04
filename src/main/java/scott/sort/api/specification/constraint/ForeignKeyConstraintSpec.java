package scott.sort.api.specification.constraint;

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
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class ForeignKeyConstraintSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlAttribute
	private String name;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> fromKey;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> toKey;
	
	public ForeignKeyConstraintSpec() {
		this.fromKey = new LinkedList<NodeSpec>();
		this.toKey = new LinkedList<NodeSpec>();
	}

	public ForeignKeyConstraintSpec(String name, Collection<NodeSpec> fromKey, EntitySpec toEntity, Collection<NodeSpec> toKey) {
		this.name = name;
		this.fromKey = fromKey;
		this.toKey = toKey;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<NodeSpec> getFromKey() {
		return fromKey;
	}

	public Collection<NodeSpec> getToKey() {
		return toKey;
	}
	
	private static String printNodes(Collection<NodeSpec> nodeSpecs) {
		StringBuilder sb = new StringBuilder();
		for (NodeSpec spec: nodeSpecs) {
			sb.append(spec.getColumnName());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Foreign Key Constraint '");
		sb.append(name);
		sb.append("' (");
		sb.append(printNodes(fromKey));
		sb.append(") REFERENCES ");
		sb.append(toKey.iterator().next().getEntity().getTableName());
		sb.append("(");
		sb.append(printNodes(toKey));
		sb.append(")");
		return sb.toString();
	}
}
