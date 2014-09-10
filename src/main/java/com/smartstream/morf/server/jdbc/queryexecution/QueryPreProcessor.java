package com.smartstream.morf.server.jdbc.queryexecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.config.NodeDefinition;
import com.smartstream.morf.api.query.QCondition;
import com.smartstream.morf.api.query.QProperty;
import com.smartstream.morf.api.query.QueryObject;

/**
 *
 * A preprocessor which allows modification of the query objects before they
 * are used to execute the query.
 *
 * The preprocessor can be used to add security constraints or other logical constraints
 * as desired.
 *
 * For example: any fixed value nodes will generate a where clause condition so that only
 * records which match those fixed values are returned.
 *
 */
public class QueryPreProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(QueryPreProcessor.class);

    /**
     * Modifies the given query
     * @param query
     * @return
     */
    public <T> QueryObject<T> preProcess(QueryObject<T> query, Definitions definitions) {
        /*
         * do nothing if we are a subquery
         */
        if (query.isSubQuery()) {
            return query;
        }

        final QCondition finalCondition = andConditions(
                query.getCondition(),
                generatePreConditionsForFixedValues(query, definitions));

        if (finalCondition != null) {
            //set the new where clause for the query
            query.where(finalCondition);
        }
        return query;
    }

    /**
     * ands the conditions, properly handling null parameters
     */
    private QCondition andConditions(QCondition aCondition, QCondition bCondition) {
        if (aCondition != null && bCondition != null) {
            return aCondition.and(bCondition);
        }
        else if (aCondition != null) {
            return aCondition;
        }
        else {
            return bCondition;
        }
    }

    private QCondition generatePreConditionsForFixedValues(QueryObject<?> query, Definitions definitions) {
        /*
         * look for fixed values which have to be set on the query
         */
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeName(), true);
        QCondition preCondition = null;
        for (NodeDefinition nd : entityType.getNodeDefinitions()) {
            if (nd.getFixedValue() != null) {
                QCondition newCondition = new QProperty<Object>(query, nd.getName()).equal(nd.getFixedValue());
                preCondition = preCondition != null ? preCondition.and(newCondition) : newCondition;
                LOG.debug("Adding to precondition for fixed value " + nd.getName() + " " + nd.getFixedValue() + " " + preCondition);
            }
        }
        return preCondition;
    }

}
