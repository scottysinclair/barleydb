package scott.barleydb.api.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import scott.barleydb.api.core.types.JavaType;

/**
 * @author scott
 *
 * @param <VAL>
 */
public class QParameter<VAL> implements Serializable {
	private final String name;
	private final JavaType type;
	private VAL value;
	
	public QParameter(String name) {
		this(name, null);
	}

	public QParameter(String name, JavaType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public VAL getValue() {
		return value;
	}

	public void setValue(VAL value) {
		this.value = value;
	}

	public JavaType getType() {
		return type;
	}
	
}
