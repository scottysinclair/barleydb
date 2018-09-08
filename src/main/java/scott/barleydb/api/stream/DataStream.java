package scott.barleydb.api.stream;

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
