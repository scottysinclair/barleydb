package scott.sort.api.core;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.queryexecution.QueryResult;

public interface IEntityContextServices {

    Definitions getDefinitions(String namespace);

    <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws Exception;

    QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws Exception;

    PersistAnalyser execute(PersistAnalyser persistAnalyser) throws Exception;
}
