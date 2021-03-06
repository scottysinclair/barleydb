package scott.barleydb.server.jdbc.query;

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

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.exception.execution.query.PreparingQueryStatementException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.helper.PreparedStatementHelper;

public class QueryPreparedStatementHelper extends PreparedStatementHelper<BarleyDBQueryException> {

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
