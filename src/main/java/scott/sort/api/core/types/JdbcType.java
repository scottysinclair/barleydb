package scott.sort.api.core.types;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public enum JdbcType {
    VARCHAR,
    NVARCHAR,
    CHAR,
    BIGINT,
    INT,
    BLOB,
    CLOB,
    DATE,
    TIMESTAMP,
    DECIMAL;

    public static boolean isStringType(JdbcType e) {
        return e == VARCHAR || e == NVARCHAR || e == CLOB;
    }
}
