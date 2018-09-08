package scott.barleydb.api.stream;

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
