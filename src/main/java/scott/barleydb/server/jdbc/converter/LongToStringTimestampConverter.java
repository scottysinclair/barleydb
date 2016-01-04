package scott.barleydb.server.jdbc.converter;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.exception.execution.TypeConversionException;

/**
 * Used to work around MySql's imprecise Timestamp format.
 * 
 * We store the timestamp as a String for MySql and use this converter
 * to handle this
 * 
 * @author scott
 *
 */
public class LongToStringTimestampConverter implements TypeConverter {
	
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSS");

	@Override
	public Object convertForwards(Object from) {
		Date date = new Date((Long)from);
		return df.format(date);
	}

	@Override
	public Object convertBackwards(Object to) throws TypeConversionException {
		Date date;
		try {
			date = df.parse((String)to);
		} 
		catch (ParseException e) {
			throw new TypeConversionException("Could not convert '" + to + "' to a date");
		}
		return date.getTime();
	}

	@Override
	public JavaType getForwardsJavaType() {
		return JavaType.STRING;
	}

	@Override
	public JavaType getBackwardsJavaType() {
		return JavaType.LONG;
	}

}
