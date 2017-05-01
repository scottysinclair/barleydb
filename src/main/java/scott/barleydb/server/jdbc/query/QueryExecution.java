package scott.barleydb.server.jdbc.query;

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.exception.execution.query.QueryConnectionRequiredException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.EntityData;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.stream.ObjectGraph;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.query.QueryGenerator.Param;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.server.jdbc.vendor.Database;

/**
 * Builds the SQL for a query
 * Processes the resultset creating the loaded entities in the entity context
 * @author scott
 *
 * @param <T>
 */
public class QueryExecution<T> {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecution.class);

    private final JdbcEntityContextServices entityContextServices;
    private final QueryObject<T> query;
    private final Definitions definitions;
    private final Projection projection;
    private final Database database;
    private EntityLoaders entityLoaders;
    private QueryGenerator qGen;

    public QueryExecution(JdbcEntityContextServices entityContextServices, EntityContext entityContext, QueryObject<T> query, Definitions definitions) throws QueryConnectionRequiredException {
        this.entityContextServices = entityContextServices;
        this.query = query;
        this.definitions = definitions;
        this.projection = new Projection(definitions);
        this.database = ConnectionResources.getMandatoryForQuery(entityContext).getDatabase();
        projection.build(query);
    }

    public String getSql(List<Param> queryParameters) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        qGen = new QueryGenerator(database, query, definitions);
        return qGen.generateSQL(projection, queryParameters);
    }


    /**
     *
     * @param resultSet
     * @param objectGraph
     * @return true if there is more data in the resultset
     * @throws EntityStreamException
     */
    public boolean readObjectGraph(ResultSet resultSet, ObjectGraph objectGraph) throws EntityStreamException {
        LOG.debug("Reading object graph from ResultSet...");

        /*
         * we are reading a whole new Object graph, we need to clear any entity data which were loaded previously.
         */
        if (entityLoaders != null) {
            entityLoaders.clearLoadedEntityData();
        }

        boolean moreData = false;
        try {

            Object rootEntityKey = null;
            do {
                prepareEntityLoadersForNewRow(resultSet);
                LOG.debug("START ROW ----------------------------------------------------------------");

                Iterator<EntityLoader> i = entityLoaders.iterator();
                EntityLoader entityDataLoader = i.next();

                if (rootEntityKey == null) {
                    //first time in the loop, lets set the root entity key
                    rootEntityKey = entityDataLoader.getEntityKey(true);
                }
                else if (!Objects.equals(rootEntityKey, entityDataLoader.getEntityKey(true))) {
                    //the PK of the root entity record has changed, so we have all of the data
                    //for the object graph...
                    moreData = true;
                    break;
                }

                /*
                 * load the root entity data if we need to.
                 */
                if (entityDataLoader.isNotYetLoaded()) {
                    entityDataLoader.load();
                }

                while(i.hasNext()) {
                    entityDataLoader = i.next();
                    if (entityDataLoader.isEntityThere()) {
                        if (entityDataLoader.isNotYetLoaded()) {
                            entityDataLoader.load();
                        }
                        else {
                            entityDataLoader.associateAsLoaded();
                        }
                    }
                }
            }
            while(resultSet.next());
            LOG.debug("-------------------------------------------------------------------");
        }
        catch (SortJdbcException  | SortQueryException  | SQLException x) {
            throw new EntityStreamException("Could not load Object Graph", x);
        }
        if (moreData) {
            LOG.debug("Fully read in Object Graph, ResultSet contains more data...");
        }
        else {
            LOG.debug("Fully read in Object Graph, reached end of ResultSet...");
        }

        prepareObjectGraphFromLoadedData( objectGraph );
        return moreData;
    }

    private ObjectGraph prepareObjectGraphFromLoadedData(ObjectGraph objectGraph) {
        objectGraph.addAll( entityLoaders.getLoadedEntityData().values() );

        setFetchedFlag(query, objectGraph);

        return objectGraph;
    }

    private void prepareEntityLoadersForNewRow(ResultSet resultSet) {
        if (entityLoaders == null) {
            entityLoaders = new EntityLoaders(entityContextServices, definitions, projection, resultSet);
        }
        else {
            entityLoaders.clearRowCache();
        }
    }

    /**
     * Sets all ToMany relations which had query joins to 'fetched'
     * @param queryObject
     * @throws IllegalQueryStateException
     */
    private void setFetchedFlag(QueryObject<?> queryObject, ObjectGraph objectGraph)  {
        //for each tomany ref set fetched to true iff the queryobject had a join for that property
        for (EntityData entityData: entityLoaders.getEntityDataLoadedFor( queryObject )) {
            EntityType entityType = definitions.getEntityTypeMatchingInterface(entityData.getEntityType(), true);
            for (QJoin join: queryObject.getJoins()) {
                String propertyName = join.getFkeyProperty();
                NodeType nodeType = entityType.getNodeType(propertyName, true);
                if (nodeType.getRelationInterfaceName() != null && nodeType.getColumnName() == null) {
                    objectGraph.setFetched(entityData.getEntityType(), getKey(entityData, entityType), propertyName);
                }
            }
        }
        //repeat the process for tomany relations in the joined queryobjects
        for (QJoin join : queryObject.getJoins()) {
            setFetchedFlag(join.getTo(), objectGraph);
        }
    }

    private Object getKey(EntityData entityData, EntityType entityType) {
        return entityData.getData().get( entityType.getKeyNodeName() );
    }

}
