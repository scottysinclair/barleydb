package scott.sort.server.jdbc.converter;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import scott.sort.api.core.types.JavaType;
import scott.sort.api.exception.execution.TypeConversionException;

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
