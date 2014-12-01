package scott.sort.server.jdbc.persist;

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
import scott.sort.api.exception.execution.persist.PreparingPersistStatementException;
import scott.sort.server.jdbc.JdbcEntityContextServices;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;

public class PersistPreparedStatementHelper extends PreparedStatementHelper<PreparingPersistStatementException> {

    public PersistPreparedStatementHelper(JdbcEntityContextServices jdbcEntityContextServices, Definitions definitions) {
        super(jdbcEntityContextServices, definitions);
    }
    
	@Override
    public PreparingPersistStatementException newPreparingStatementException(String message) {
        return new PreparingPersistStatementException(message);
    }

    @Override
    public PreparingPersistStatementException newPreparingStatementException(String message, Throwable cause) {
        return new PreparingPersistStatementException(message, cause);
    }

}
