package scott.barleydb.api.stream;

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

import java.io.Closeable;
import java.io.IOException;

import scott.barleydb.api.core.entity.Entity;

public class ObjectInputStream implements Closeable {

    private final EntityInputStream in;

    public ObjectInputStream(EntityInputStream in) {
        this.in = in;
    }

    /**
     * Reads an Object from the stream.<br/>
     * It may be a single object or the root of an object graph.
     * @return the object or null if the end of the stream is reached.
     */
    public Object read() {
        Entity entity = in.read();
        return entity.getEntityContext().getProxy(entity);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
