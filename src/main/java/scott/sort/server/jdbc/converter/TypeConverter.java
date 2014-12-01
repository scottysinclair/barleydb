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

import scott.sort.api.core.types.JavaType;
import scott.sort.api.exception.execution.TypeConversionException;

public interface TypeConverter {
	
	Object convertForwards(Object from) throws TypeConversionException;
	
	Object convertBackwards(Object to) throws TypeConversionException;

	JavaType getForwardsJavaType();

	JavaType getBackwardsJavaType();
	
}
