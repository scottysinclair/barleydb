package scott.sort.api.exception.query;

public class QueryConnectionRequiredException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public QueryConnectionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryConnectionRequiredException(String message) {
        super(message);
    }


}
