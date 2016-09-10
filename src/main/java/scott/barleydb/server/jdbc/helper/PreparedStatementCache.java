package scott.barleydb.server.jdbc.helper;

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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.jdbc.ClosingStatementException;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.server.jdbc.resources.ConnectionResources;

public abstract class PreparedStatementCache<PREPARING_PERSIST_EX extends SortException, CONNECTION_REQ_EX extends SortException> implements AutoCloseable {

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
                throw helper.newPreparingStatementException("SQLException preparing statement", x);
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
                throw helper.newPreparingStatementException("SQLException preparing statement", x);
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
                throw helper.newPreparingStatementException("SQLException preparing statement", x);
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
            final NodeType nd = entity.getEntityType().getNodeType(child.getName(), true);
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
            final NodeType nd = entity.getEntityType().getNodeType(child.getName(), true);
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
            sb.append(entity.getOptimisticLock().getNodeType().getColumnName());
            sb.append(" = ?");
        }
    }

    private void setInsertParameters(PreparedStatement ps, Entity entity, Long newOptimisticLockTime) throws PREPARING_PERSIST_EX {
        int i = 1;
        for (final Node child : entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            if (child.getNodeType().isOptimisticLock() && !helper.getRuntimeProps().isDisabledOptimisticLocks()) {
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
            final NodeType nd = entity.getEntityType().getNodeType(child.getName(), true);
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
