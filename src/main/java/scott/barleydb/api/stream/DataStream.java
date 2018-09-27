package scott.barleydb.api.stream;

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

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import scott.barleydb.api.exception.BarleyDBRuntimeException;

public interface DataStream<T> extends AutoCloseable {

    T read() throws EntityStreamException;

    void close() throws EntityStreamException;

    default Stream<T> stream() {
        return StreamSupport.stream(new BarleyDbSpliterator<T>(this), false)
            .onClose(() -> {
                try { close(); }
                catch(EntityStreamException x) {
                    BarleyDBRuntimeException xr = new BarleyDBRuntimeException(x.getMessage());
                    xr.setStackTrace(x.getStackTrace());
                    throw xr;
                }
            });
    }
}
