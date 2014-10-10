package scott.sort.server.jdbc.persister;

import scott.sort.api.config.Definitions;
import scott.sort.api.exception.persist.PreparingPersistStatementException;
import scott.sort.server.jdbc.helper.PreparedStatementHelper;

public class PersistPreparedStatementHelper extends PreparedStatementHelper<PreparingPersistStatementException> {

    public PersistPreparedStatementHelper(Definitions definitions) {
        super(definitions);
    }

    @Override
    public PreparingPersistStatementException newPreparingPersistStatementException(String message) {
        return new PreparingPersistStatementException(message);
    }

    @Override
    public PreparingPersistStatementException newPreparingPersistStatementException(String message, Throwable cause) {
        return new PreparingPersistStatementException(message, cause);
    }

}
