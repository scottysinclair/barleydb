package scott.barleydb.api.graphql;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import graphql.GraphQLError;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

import java.util.List;

public class GraphQLExecutionException extends BarleyDBRuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<GraphQLError> errors;

    public GraphQLExecutionException(List<GraphQLError> errors) {
        this.errors = errors;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        return errors.toString();
    }
}
