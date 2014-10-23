package scott.sort.server.jdbc.queryexecution;

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
import scott.sort.api.exception.execution.query.PreparingQueryStatementException;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;

public class QueryPreparedStatementHelper extends PreparedStatementHelper<PreparingQueryStatementException> {

    public QueryPreparedStatementHelper(Definitions definitions) {
        super(definitions);
    }

    @Override
    public PreparingQueryStatementException newPreparingPersistStatementException(String message) {
        return new PreparingQueryStatementException(message);
    }

    @Override
    public PreparingQueryStatementException newPreparingPersistStatementException(String message, Throwable cause) {
        return new PreparingQueryStatementException(message, cause);
    }

}
