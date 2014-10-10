package scott.sort.server.jdbc.persister;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.*;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.*;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.exception.ClosingStatementException;
import scott.sort.api.exception.SortException;
import scott.sort.server.jdbc.helper.*;
import scott.sort.server.jdbc.resources.ConnectionResources;

abstract class PreparedStatementCache<PREPARING_PERSIST_EX extends SortException, CONNECTION_REQ_EX extends SortException> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PreparedStatementCache.class);

    private final Map<EntityType, PreparedStatement> inserts = new HashMap<>();
    private final Map<EntityType, PreparedStatement> updates = new HashMap<>();
    private final Map<EntityType, PreparedStatement> deletes = new HashMap<>();

    private final PreparedStatementHelper<PREPARING_PERSIST_EX> helper;

    public PreparedStatementCache(PreparedStatementHelper<PREPARING_PERSIST_EX> helper) {
        this.helper = helper;
    }

    public PreparedStatement prepareInsertStatement(Entity entity, Long newOptimisticLockTime) throws PREPARING_PERSIST_EX, CONNECTION_REQ_EX {
        PreparedStatement ps = inserts.get(entity.getEntityType());
        if (ps == null) {
            ConnectionResources conRes = getConnectionResources(entity.getEntityContext());
            try {
                ps = conRes.getConnection().prepareStatement(generateInsertSql(entity));
            }
            catch (SQLException x) {
                throw helper.newPreparingPersistStatementException("SQLException preparing statement", x);
            }
            inserts.put(entity.getEntityType(), ps);
        }
        setInsertParameters(ps, entity, newOptimisticLockTime);
        return ps;
    }

    public PreparedStatement prepareUpdateStatement(Entity entity, Long newOptimisticLockTime) throws PREPARING_PERSIST_EX, CONNECTION_REQ_EX {
        PreparedStatement ps = updates.get(entity.getEntityType());
        if (ps == null) {
            ConnectionResources conRes = getConnectionResources(entity.getEntityContext());
            try {
                ps = conRes.getConnection().prepareStatement(generateUpdateSql(entity));
                updates.put(entity.getEntityType(), ps);
            }
            catch(SQLException x) {
                throw helper.newPreparingPersistStatementException("SQLException preparing statement", x);
            }
        }
        setUpdateParameters(ps, entity, newOptimisticLockTime);
        return ps;
    }

    public PreparedStatement prepareDeleteStatement(Entity entity) throws PREPARING_PERSIST_EX, CONNECTION_REQ_EX {
        PreparedStatement ps = deletes.get(entity.getEntityType());
        if (ps == null) {
            ConnectionResources conRes = getConnectionResources(entity.getEntityContext());
            try {
                ps = conRes.getConnection().prepareStatement(generateDeleteSql(entity));
            }
            catch (SQLException x) {
                throw helper.newPreparingPersistStatementException("SQLException preparing statement", x);
            }
            deletes.put(entity.getEntityType(), ps);
        }
        setDeleteParameters(ps, entity);
        return ps;
    }

    protected abstract ConnectionResources getConnectionResources(EntityContext entityContetx) throws CONNECTION_REQ_EX;

    @Override
    public void close() throws ClosingStatementException {
        ClosingStatementException x = null;
        for (PreparedStatement ps : inserts.values()) {
            try {
                ps.close();
            }
            catch (SQLException e) {
              if (x == null) {
                  x = new ClosingStatementException("SQLException closing prepared statement", e);
              }
            }
        }
        for (PreparedStatement ps : updates.values()) {
            try {
                ps.close();
            }
            catch (SQLException e) {
                if (x == null) {
                    x = new ClosingStatementException("SQLException closing prepared statement", e);
                }
            }
        }
        for (PreparedStatement ps : deletes.values()) {
            try {
                ps.close();
            }
            catch (SQLException e) {
                if (x == null) {
                    x = new ClosingStatementException("SQLException closing prepared statement", e);
                }
            }
        }
        if (x != null) {
            throw x;
        }
    }

    private String generateInsertSql(Entity entity) {
        StringBuilder sb = new StringBuilder("insert into ");
        sb.append(entity.getEntityType().getTableName());
        sb.append(' ');
        sb.append('(');
        for (Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            final NodeDefinition nd = entity.getEntityType().getNode(child.getName(), true);
            sb.append(nd.getColumnName());
            sb.append(',');
        }
        sb.setCharAt(sb.length() - 1, ')');
        sb.append("values (");
        for (final Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            sb.append("?,");
        }
        sb.setCharAt(sb.length() - 1, ')');
        LOG.debug(sb.toString());
        return sb.toString();
    }

    private String generateUpdateSql(Entity entity) {
        StringBuilder sb = new StringBuilder("update ");
        sb.append(entity.getEntityType().getTableName());
        sb.append(" set ");
        for (Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            final NodeDefinition nd = entity.getEntityType().getNode(child.getName(), true);
            if (!nd.isPrimaryKey()) {
                sb.append(nd.getColumnName());
                sb.append(" = ?,");
            }
        }
        sb.setCharAt(sb.length() - 1, ' ');
        generateDeleteOrUpdateWhere(entity, sb);
        LOG.debug(sb.toString());
        return sb.toString();
    }

    private String generateDeleteSql(Entity entity) {
        StringBuilder sb = new StringBuilder("delete from ");
        sb.append(entity.getEntityType().getTableName());
        generateDeleteOrUpdateWhere(entity, sb);
        LOG.debug(sb.toString());
        return sb.toString();
    }

    private void generateDeleteOrUpdateWhere(Entity entity, StringBuilder sb) {
        sb.append(" where ");
        sb.append(entity.getEntityType().getKeyColumn());
        sb.append(" = ?");
        if (entity.getEntityType().supportsOptimisticLocking()) {
            sb.append(" and ");
            sb.append(entity.getOptimisticLock().getNodeDefinition().getColumnName());
            sb.append(" = ?");
        }
    }

    private void setInsertParameters(PreparedStatement ps, Entity entity, Long newOptimisticLockTime) throws PREPARING_PERSIST_EX {
        int i = 1;
        for (final Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            if (child.getNodeDefinition().isOptimisticLock()) {
                //we set the new optimistic lock value, the OL node still contains the old value
                helper.setParameter(ps, i++, child, newOptimisticLockTime);
            }
            else {
                helper.setParameter(ps, i++, child);
            }
        }
    }

    private void setUpdateParameters(PreparedStatement ps, Entity entity, Long newOptimisticLockTime) throws PREPARING_PERSIST_EX {
        int i = 1;
        for (final Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            final NodeDefinition nd = entity.getEntityType().getNode(child.getName(), true);
            if (!nd.isPrimaryKey()) {
                if (nd.isOptimisticLock()) {
                    //we set the new optimistic lock value, the OL node still contains the old value
                    helper.setParameter(ps, i++, child, newOptimisticLockTime);
                }
                else {
                    helper.setParameter(ps, i++, child);
                }
            }
        }
        helper.setParameter(ps, i++, entity.getKey());
        if (entity.getEntityType().supportsOptimisticLocking()) {
            helper.setParameter(ps, i++, entity.getOptimisticLock());
        }
    }

    private void setDeleteParameters(PreparedStatement ps, Entity entity) throws PREPARING_PERSIST_EX {
        helper.setParameter(ps, 1, entity.getKey());
        Node olNode = entity.getOptimisticLock();
        if (olNode != null) {
            helper.setParameter(ps, 2, olNode);
        }
    }
}
