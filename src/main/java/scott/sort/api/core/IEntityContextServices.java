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

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.SortServiceProviderException;
import scott.sort.api.exception.execution.persist.SortPersistException;
import scott.sort.api.exception.execution.query.SortQueryException;
import scott.sort.api.query.QueryObject;
import scott.sort.api.query.RuntimeProperties;
import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.queryexecution.QueryResult;

/**
 * Interface for performing queries and persisting entities
 * @author scott
 *
 */
public interface IEntityContextServices {

    /**
     * Sets the auto commit mode of the entity context.
     *
     * If the mode changes to true, then any ongoing transaction will be committed.
     *
     * @param entityContext
     * @param value
     * @throws SortJdbcException if an access error occurred, if an attempted commit failed.
     *  if the auto commit mode is not supported by the environment.
     */
    void setAutocommit(EntityContext entityContext, boolean value) throws SortServiceProviderException;

    /**
     * Gets the auto commit mode of the entity context.
     * @param entityContext
     * @return
     * @throws SortJdbcException if an access error occurred.
     */
    boolean getAutocommit(EntityContext entityContext) throws SortServiceProviderException;

    /**
     * Makes the newContext join the same transaction as contextWithTransaction.<br/>
     *
     * If contextWithTransaction has no connection resources associated to it then this is a noop.
     *
     * @param newContext
     * @param contextWithTransaction
     */
    void joinTransaction(EntityContext newContext, EntityContext contextWithTransaction);

    /**
     * Commits the transaction associated with the entity context
     *
     * @param entityContext
     * @throws SortJdbcException if the commit failed, if we are in auto commit mode.
     */
    void commit(EntityContext entityContext) throws SortServiceProviderException;

    /**
     * Rolls the transaction for the given entity context back.
     * @param entityContext
     * @throws SortJdbcException
     */
    void rollback(EntityContext entityContext) throws SortServiceProviderException;

    /**
     * Executes the query populating the given entity context with the result.
     *
     * @param namespace
     * @param entityContext
     * @param query
     * @param props
     * @return
     * @throws SortJdbcException
     * @throws SortQueryException
     */
    <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortServiceProviderException, SortQueryException;

    /**
     * Executes the batch of queries populating the given entity context with the result.
     *
     * @param namespace
     * @param entityContext
     * @param queryBatcher
     * @param props
     * @return
     * @throws SortJdbcException
     * @throws SortQueryException
     */
    QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher, RuntimeProperties props) throws SortServiceProviderException, SortQueryException;

    /**
     * Executes the persist analysis
     *
     * @param persistAnalyser
     * @return
     * @throws SortJdbcException
     * @throws SortPersistException
     */
    PersistAnalyser execute(PersistAnalyser persistAnalyser) throws SortServiceProviderException, SortPersistException;
}
