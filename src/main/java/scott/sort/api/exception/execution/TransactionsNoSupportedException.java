package scott.sort.api.exception.execution;

/**
 * Thrown if transactions are not supported in the environment
 * @author scott
 *
 */
public class TransactionsNoSupportedException extends SortServiceProviderException {

    private static final long serialVersionUID = 1L;

    public TransactionsNoSupportedException() {
        super();
    }

    public TransactionsNoSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TransactionsNoSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionsNoSupportedException(String message) {
        super(message);
    }

    public TransactionsNoSupportedException(Throwable cause) {
        super(cause);
    }

}
