package scott.sort.api.core;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.Definitions;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.query.SortQueryException;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.persister.exception.SortPersistException;
import scott.sort.server.jdbc.queryexecution.QueryResult;

public interface IEntityContextServices {

    void setAutocommit(EntityContext entityContext, boolean value) throws SortJdbcException;

    boolean getAutocommit(EntityContext entityContext) throws SortJdbcException;

    void joinTransaction(EntityContext newContext, EntityContext contextWithTransaction);

    void rollback(EntityContext entityContext) throws SortJdbcException;

    Definitions getDefinitions(String namespace);

    <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws SortJdbcException, SortQueryException;

    QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws SortJdbcException, SortQueryException;

    PersistAnalyser execute(PersistAnalyser persistAnalyser) throws SortJdbcException, SortPersistException;
}
