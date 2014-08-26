package com.smartstream.morf.server.jdbc.persister;

import java.util.*;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.*;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.Node;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.server.jdbc.helper.*;

class PreparedStatementCache {

    private static final Logger LOG = LoggerFactory.getLogger(PreparedStatementCache.class);

    private final Map<EntityType,PreparedStatement> inserts = new HashMap<>();
    private final Map<EntityType,PreparedStatement> updates = new HashMap<>();
    private final Map<EntityType,PreparedStatement> deletes = new HashMap<>();

    private final PreparedStatementHelper helper;

    public PreparedStatementCache(Definitions definitions) {
        this.helper = new PreparedStatementHelper(definitions);
    }

    public PreparedStatement prepareInsertStatement(Entity entity, Long newOptimisticLockTime) throws SQLException {
      PreparedStatement ps = inserts.get( entity.getEntityType() );
      if (ps == null) {
    	  Connection connection = (Connection)entity.getEntityContext().getResource(Connection.class.getName(), true);
          ps = connection.prepareStatement( generateInsertSql(entity) );
          inserts.put(entity.getEntityType(), ps);
      }
      setInsertParameters(ps, entity, newOptimisticLockTime);
      return ps;
    }

    public PreparedStatement prepareUpdateStatement(Entity entity, Long newOptimisticLockTime) throws SQLException {
      PreparedStatement ps = updates.get( entity.getEntityType() );
      if (ps == null) {
    	  Connection connection = (Connection)entity.getEntityContext().getResource(Connection.class.getName(), true);
          ps = connection.prepareStatement( generateUpdateSql(entity) );
          updates.put(entity.getEntityType(), ps);
      }
      setUpdateParameters(ps, entity, newOptimisticLockTime);
      return ps;
    }

    public PreparedStatement prepareDeleteStatement(Entity entity) throws SQLException {
      PreparedStatement ps = deletes.get( entity.getEntityType() );
      if (ps == null) {
    	  Connection connection = (Connection)entity.getEntityContext().getResource(Connection.class.getName(), true);
          ps = connection.prepareStatement( generateDeleteSql(entity) );
          deletes.put(entity.getEntityType(), ps);
      }
      setDeleteParameters(ps, entity);
      return ps;
    }

    public void close() throws SQLException {
    	for (PreparedStatement ps: inserts.values()) {
    		ps.close();
    	}
    	for (PreparedStatement ps: updates.values()) {
    		ps.close();
    	}
    	for (PreparedStatement ps: deletes.values()) {
    		ps.close();
    	}
    }

    private String generateInsertSql(Entity entity) {
        StringBuilder sb = new StringBuilder("insert into ");
        sb.append( entity.getEntityType().getTableName());
        sb.append(' ');
        sb.append('(');
        for (Node child: entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            final NodeDefinition nd = entity.getEntityType().getNode(child.getName(), true);
            sb.append(nd.getColumnName());
            sb.append(',');
        }
        sb.setCharAt(sb.length()-1, ')');
        sb.append("values (");
        for (final Node child: entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            sb.append("?,");
        }
        sb.setCharAt(sb.length()-1, ')');
        LOG.debug(sb.toString());
        return sb.toString();
    }

    private String generateUpdateSql(Entity entity) {
        StringBuilder sb = new StringBuilder("update ");
        sb.append( entity.getEntityType().getTableName());
        sb.append(" set ");
        for (Node child: entity.getChildren()) {
            if (child instanceof ToManyNode) {
                continue;
            }
            final NodeDefinition nd = entity.getEntityType().getNode(child.getName(), true);
            if (!nd.isPrimaryKey()) {
                sb.append(nd.getColumnName());
                sb.append(" = ?,");
            }
        }
        sb.setCharAt( sb.length()-1, ' ');
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
        	sb.append( entity.getOptimisticLock().getNodeDefinition().getColumnName() );
        	sb.append(" = ?");
        }
    }

    private void setInsertParameters(PreparedStatement ps, Entity entity, Long newOptimisticLockTime) throws SQLException {
		int i = 1;
		for (final Node child: entity.getChildren()) {
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

    private void setUpdateParameters(PreparedStatement ps, Entity entity, Long newOptimisticLockTime) throws SQLException {
		int i = 1;
		for (final Node child: entity.getChildren()) {
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

    private void setDeleteParameters(PreparedStatement ps, Entity entity) throws SQLException {
        helper.setParameter(ps, 1, entity.getKey());
        Node olNode = entity.getOptimisticLock();
        if (olNode != null) {
        	helper.setParameter(ps, 2, olNode);
        }
    }
}
