package com.smartstream.morf.api.core.types;

public enum JdbcType {
    VARCHAR,
    NVARCHAR,
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
