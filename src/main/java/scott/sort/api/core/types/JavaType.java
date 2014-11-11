package scott.sort.api.core.types;

import java.math.BigDecimal;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public enum JavaType {
    INTEGER(Integer.class),
    LONG(Long.class),
    BIGDECIMAL(BigDecimal.class),
    STRING(String.class),
    UTIL_DATE(java.util.Date.class),
    SQL_DATE(java.sql.Date.class),
    BOOLEAN(Boolean.class),
    ENUM(null),
    UUID(java.util.UUID.class);

    private final Class<?> javaTypeClass;

    private JavaType(Class<?> javaTypeClass) {
        this.javaTypeClass = javaTypeClass;
    }

    public Class<?> getJavaTypeClass() {
        return javaTypeClass;
    }

}
