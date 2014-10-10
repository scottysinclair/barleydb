package scott.sort.api.exception;

/**
 * Class for all exceptions which come from the JDBC drivers
 * ie all of the SQLExceptions
 * @author scott
 *
 */
public class SortJdbcException extends SortException {

    private static final long serialVersionUID = 1L;

    public SortJdbcException() {
        super();
    }

    public SortJdbcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortJdbcException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortJdbcException(String message) {
        super(message);
    }

    public SortJdbcException(Throwable cause) {
        super(cause);
    }

}
