package scott.barleydb.api.stream;

import java.util.Iterator;

public class StreamingObjectIterator<T> implements Iterator<T>, AutoCloseable {

    private final ObjectInputStream<T> in;
    private T next;

    public StreamingObjectIterator(ObjectInputStream<T> in) throws EntityStreamException {
        this.in = in;
        next = in.read();
    }

    @Override
    public boolean hasNext() {
        return next != null;
   }

    @Override
    public T next() {
        T value = next;
        try {
            next = in.read();
        }
        catch (EntityStreamException x) {
            throw new IllegalStateException("Error reading next object", x);
        }
        return value;
    }

    @Override
    public void close() throws Exception {
        in.close();
    }




}
