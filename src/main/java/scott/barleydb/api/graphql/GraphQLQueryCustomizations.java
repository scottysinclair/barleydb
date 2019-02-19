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

import java.util.function.BiPredicate;

import scott.barleydb.api.query.QJoin;

/**
 * Customizes the query execution of graphql
 * @author scott
 *
 */
public class GraphQLQueryCustomizations {
	
	private BiPredicate<QJoin, GraphQLContext> shouldBreakPredicate;

	public boolean shouldBreakJoin(QJoin join, GraphQLContext graphCtx) {
		if (shouldBreakPredicate == null) {
			return false;
		}
		return shouldBreakPredicate.test(join, graphCtx);
	}

	public void setShouldBreakPredicate(BiPredicate<QJoin, GraphQLContext> shouldBreakPredicate) {
		this.shouldBreakPredicate = shouldBreakPredicate;
	}

	public GraphQLQueryCustomizations copy() {
		return new GraphQLQueryCustomizations();
	}
	
	
}
