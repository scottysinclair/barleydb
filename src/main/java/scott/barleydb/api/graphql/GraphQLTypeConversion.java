package scott.barleydb.api.graphql;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.types.JavaType;

public class GraphQLTypeConversion {

	public static Object convertValue(NodeType nodeType, Object value) {
		return convertValue(value, nodeType.getJavaType());
	}

	public static Object convertValue(Object value, JavaType javaType) {
		switch (javaType) {
			case LONG:
				return convertToLong(value);
			case UTIL_DATE: 
				return convertToUtilDate(value);
			default: return value;
		}
	}
	
	private static final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	private static synchronized Date parse(String dateString) {
		try {
			return df.parse(dateString);
		} catch (ParseException e) {
			throw new IllegalStateException(String.format("Cannot convert %s to Util Date", dateString));
		}
	}
	private static Object convertToUtilDate(Object value) {
		if (value instanceof String) {
			return parse((String)value);
		}
		return value;
	}

	public static Long convertToLong(Object value) {
		if (value instanceof Integer) {
			return ((Integer)value).longValue();
		}
		throw new IllegalStateException(String.format("Cannot convert %s to Long", value));
	}

}
