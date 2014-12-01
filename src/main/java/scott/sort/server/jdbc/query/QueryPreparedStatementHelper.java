package scott.sort.server.jdbc.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.Definitions;
import scott.sort.api.exception.execution.query.SortQueryException;
import scott.sort.api.exception.execution.query.PreparingQueryStatementException;
import scott.sort.server.jdbc.JdbcEntityContextServices;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;

public class QueryPreparedStatementHelper extends PreparedStatementHelper<SortQueryException> {

    public QueryPreparedStatementHelper(JdbcEntityContextServices jdbcEntityContextServices, Definitions definitions) {
        super(jdbcEntityContextServices, definitions);
    }
    
	@Override
    public PreparingQueryStatementException newPreparingStatementException(String message) {
        return new PreparingQueryStatementException(message);
    }

    @Override
    public PreparingQueryStatementException newPreparingStatementException(String message, Throwable cause) {
        return new PreparingQueryStatementException(message, cause);
    }

}
