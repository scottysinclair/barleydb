package com.smartstream.morf.api.core;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.api.query.QueryObject;
import com.smartstream.morf.server.jdbc.persister.PersistAnalyser;
import com.smartstream.morf.server.jdbc.queryexecution.QueryResult;

public interface IEntityContextServices {

	Definitions getDefinitions(String namespace);

	<T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws Exception;

	QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws Exception;

	PersistAnalyser execute(PersistAnalyser persistAnalyser) throws Exception;
}
