package scott.barleydb.api.stream;

import java.util.Iterator;

/**
 * helpfull class which provides an iterator to a stream  of objects and allows it to be closed.
 * @author scott
 *
 * @param <T>
 */
public class IterableObjectStream<T> implements Iterable<T>, AutoCloseable {

    private final StreamingObjectIterator<T> it;

    public IterableObjectStream(StreamingObjectIterator<T> it) {
        this.it = it;
    }

    @Override
    public void close() throws Exception {
        it.close();
    }

    @Override
    public Iterator<T> iterator() {
        return it;
    }


}
