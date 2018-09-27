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

import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import scott.barleydb.api.exception.BarleyDBRuntimeException;

public class BarleyDbSpliterator<T> extends AbstractSpliterator<T> {

    private DataStream<T> readable;
    private boolean finished;

    public BarleyDbSpliterator(DataStream<T> readable) {
        super(Long.MAX_VALUE, Spliterator.DISTINCT);
        this.readable = readable;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        try {
            if (finished) {
                return false;
            }
            T item = readable.read();
            if (item != null) {
                action.accept(item);
                return true;
            }
            else {
                finished = true;
                return true;
            }
        }
        catch(EntityStreamException x) {
            BarleyDBRuntimeException x2 =  new BarleyDBRuntimeException("Error reading from entity stream", x);
            x2.setStackTrace(x.getStackTrace());
            throw x2;
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }




}
