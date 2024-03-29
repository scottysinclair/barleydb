package scott.barleydb.api.graphql;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.query.QJoin;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
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

public interface GraphQLContext {

	<T> T execute(String body);
	
	GraphQLQueryCustomizations getQueryCustomizations();

	void registerJoinBreak(QJoin join);

	QJoin getJoinBreakFor(Entity entity, String property);
	
	<T> T get(String key);
	
	Object put(String key, Object value);

	void setBatchFetchEnabled(boolean batchFetchEnabled);

	boolean isBatchFetchEnabled();

}
