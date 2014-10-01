package com.smartstream.sort.api.core;

import com.smartstream.sort.api.config.Definitions;
import com.smartstream.sort.api.core.entity.EntityContext;
import com.smartstream.sort.api.query.QueryObject;
import com.smartstream.sort.server.jdbc.persister.PersistAnalyser;
import com.smartstream.sort.server.jdbc.queryexecution.QueryResult;

public interface IEntityContextServices {

    Definitions getDefinitions(String namespace);

    <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws Exception;

    QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws Exception;

    PersistAnalyser execute(PersistAnalyser persistAnalyser) throws Exception;
}
