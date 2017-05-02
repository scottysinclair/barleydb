package scott.barleydb.api.core;

import scott.barleydb.api.audit.AuditInformation;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import scott.barleydb.api.config.DefinitionsSet;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.api.core.QueryBatcher;

/**
 * Interface for performing queries and persisting entities
 * @author scott
 *
 */
public interface IEntityContextServices {

    /**
     * Gets all of the definitions of the environment which this
     * entity context services executes in.
     * @return
     */
    public DefinitionsSet getDefinitionsSet();

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
    void setAutoCommit(EntityContext entityContext, boolean value) throws SortServiceProviderException;

    /**
     * Gets the auto commit mode of the entity context.
     * @param entityContext
     * @return
     * @throws SortJdbcException if an access error occurred.
     */
    boolean getAutoCommit(EntityContext entityContext) throws SortServiceProviderException;

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
     * Executes the query returning a stream of EntityData
     * @param entityContext
     * @param query
     * @param props
     * @return
     * @throws SortJdbcException
     * @throws SortQueryException
     */
    <T> QueryEntityDataInputStream streamQuery(EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortJdbcException, SortQueryException;



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
    <T> QueryResult<T> execute(EntityContext entityContext, QueryObject<T> query, RuntimeProperties props) throws SortServiceProviderException, SortQueryException;

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
    QueryBatcher execute(EntityContext entityContext, QueryBatcher queryBatcher, RuntimeProperties props) throws SortServiceProviderException, SortQueryException;


    /**
     * Generates audit information for a persist request. Can be used to see if there are any changes.
     *
     * @param persistRequest
     * @param runtimeProperties
     * @return
     * @throws SortJdbcException
     * @throws SortPersistException
     */
    AuditInformation comapreWithDatabase(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortJdbcException, SortPersistException;
    /**
     * Executes the persist analysis
     *
     * @param persistAnalyser
     * @return
     * @throws SortJdbcException
     * @throws SortPersistException
     */
    PersistAnalyser execute(PersistRequest persistRequest, RuntimeProperties props) throws SortServiceProviderException, SortPersistException;
}
