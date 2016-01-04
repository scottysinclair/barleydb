package scott.sort.api.exception.execution.jdbc;

import scott.sort.api.exception.execution.SortServiceProviderException;

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

/**
 * Class for all exceptions which come from the JDBC drivers
 * ie all of the SQLExceptions
 * @author scott
 *
 */
public class SortJdbcException extends SortServiceProviderException {

    private static final long serialVersionUID = 1L;

    public SortJdbcException() {
        super();
    }

    public SortJdbcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortJdbcException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortJdbcException(String message) {
        super(message);
    }

    public SortJdbcException(Throwable cause) {
        super(cause);
    }

}
