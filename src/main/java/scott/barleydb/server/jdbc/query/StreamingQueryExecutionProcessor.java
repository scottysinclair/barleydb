package scott.barleydb.server.jdbc.query;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
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

import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.api.stream.QueryResultItem;
import scott.barleydb.server.jdbc.query.QueryExecuter.IResultManager;

/**
 * A stream of Entities loaded from the query results
 * @author scott
 *
 */
public class StreamingQueryExecutionProcessor implements QueryEntityDataInputStream {

    /**
     * Abstracts the ResultSet for us.
     */
    private IResultManager resultManager;

    public StreamingQueryExecutionProcessor(IResultManager resultManager) throws EntityStreamException {
        this.resultManager = resultManager;
        resultManager.next();
    }

    /**
     * @return the next QueryResult
     */
    @Override
    public QueryResultItem read() throws EntityStreamException {
        if (!resultManager.hasNext()) {
            return null;
        }
        QueryResultItem resultItem = new QueryResultItem();

        resultItem.setQueryIndex( resultManager.getQueryIndex() );

        resultItem.setObjectGraph( resultManager.readObjectGraph() );

        return resultItem;
    }

    @Override
    public void close() throws EntityStreamException {
        resultManager.close();
    }

}
