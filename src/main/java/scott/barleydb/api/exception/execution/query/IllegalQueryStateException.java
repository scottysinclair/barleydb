package scott.barleydb.api.exception.execution.query;

import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;

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


public class IllegalQueryStateException extends BarleyDBQueryException {

    private static final long serialVersionUID = 1L;

    public IllegalQueryStateException() {
        super();
    }

    public IllegalQueryStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public IllegalQueryStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalQueryStateException(String message) {
        super(message);
    }

    public IllegalQueryStateException(Throwable cause) {
        super(cause);
    }

}
