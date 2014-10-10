package scott.sort.api.exception.query;



/**
 * An exception occurred preparing statements either from the JDBC driver
 * or in the sort logic.
 *
 * @author scott
 *
 */
public class PreparingQueryStatementException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public PreparingQueryStatementException() {
    }

    public PreparingQueryStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PreparingQueryStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreparingQueryStatementException(String message) {
        super(message);
    }

    public PreparingQueryStatementException(Throwable cause) {
        super(cause);
    }


}
