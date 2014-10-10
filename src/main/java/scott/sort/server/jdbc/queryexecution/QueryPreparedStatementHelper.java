package scott.sort.server.jdbc.queryexecution;

import scott.sort.api.config.Definitions;
import scott.sort.api.exception.query.PreparingQueryStatementException;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;

public class QueryPreparedStatementHelper extends PreparedStatementHelper<PreparingQueryStatementException> {

    public QueryPreparedStatementHelper(Definitions definitions) {
        super(definitions);
    }

    @Override
    public PreparingQueryStatementException newPreparingPersistStatementException(String message) {
        return new PreparingQueryStatementException(message);
    }

    @Override
    public PreparingQueryStatementException newPreparingPersistStatementException(String message, Throwable cause) {
        return new PreparingQueryStatementException(message, cause);
    }

}
