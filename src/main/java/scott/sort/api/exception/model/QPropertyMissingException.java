package scott.sort.api.exception.model;

import scott.sort.api.query.QueryObject;

public class QPropertyMissingException extends SortQueryModelException {

    private static final long serialVersionUID = 1L;

    public QPropertyMissingException(QueryObject<?> queryObject, String propertyName) {
        super("Could not find property '" + propertyName + "' for queryObject '" + queryObject + "'");

    }

}
