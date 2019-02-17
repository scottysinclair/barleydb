package scott.barleydb.api.graphql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;

/**
 * breaks a query on a join based on the following rules.
 * <ul>
 *  <li>If a query has more than one join from it and the previous joins have their own nested joins</li>
 * </ul>
 * @author scott
 *
 */
public class DefaultQueryBreaker implements BiPredicate<QJoin, GraphQLContext> {
	
	private final Environment env;
	private final String namespace;
	private final int maximumDepth;
	/**
	 * types which we force a break when joining to
	 */
	private final Set<String> forceBreakSet;
	
	public DefaultQueryBreaker(Environment env, String namespace,int maximumDepth) {
		this(env, namespace, maximumDepth, null);
	}
	public DefaultQueryBreaker(Environment env, String namespace,int maximumDepth, Collection<String> forceBreak) {
		this.env = env;
		this.namespace = namespace;
		this.maximumDepth = maximumDepth;
		this.forceBreakSet = new HashSet<>(forceBreak != null ? forceBreak : Collections.emptyList());
	}

	@Override
	public boolean test(QJoin qjoin, GraphQLContext gctx) {
	    Set<QJoin> joinsToBreak = gctx.get(DefaultQueryBreaker.class.getName());
	    if (joinsToBreak == null) {
	    	joinsToBreak = calculateJoinsToBreak(qjoin);
	    	gctx.put(DefaultQueryBreaker.class.getName(), joinsToBreak);
	    }
	    return joinsToBreak.contains(qjoin);
	}
	
	private Set<QJoin> calculateJoinsToBreak(QJoin qjoin) {
		Set<QJoin> result = new HashSet<>();
		QueryObject<?> queryObject = getRootQuery(qjoin);
		ArrayList<QJoin> allJoins = getOneToManyJoins(queryObject, true, new ArrayList<>());
		for (int i=0; i<allJoins.size(); i++) {
			if (i == 0) {
				continue;
			}
			if (forceBreakSet.contains(allJoins.get(i).getTo().getTypeName())) {
				result.add(allJoins.get(i));
			}
			else if (i % maximumDepth == 0) {
				result.add(allJoins.get(i));
			}
		}
		return result;
	}

	private QueryObject<?> getRootQuery(QJoin qjoin) {
		if (qjoin.getFrom().getJoined() == null) {
			return qjoin.getFrom();
		}
		return getRootQuery(qjoin.getFrom().getJoined());
	}

	private ArrayList<QJoin> getOneToManyJoins(QueryObject<?> queryObject, boolean includeNested, ArrayList<QJoin> result) {
		for (QJoin qj: queryObject.getJoins()) {
			if (isOneToMany(qj)) {
				result.add(qj);
			}
			if (includeNested) {
				getOneToManyJoins(qj.getTo() ,  true, result);
			}
		}
		return result;
	}

	private boolean isOneToMany(QJoin qj) {
		EntityType entityType = env.getDefinitions(namespace).getEntityTypeMatchingInterface(qj.getFrom().getTypeName(), true);
		return entityType.getNodeType(qj.getFkeyProperty(), true).isOneToManyRelation();
	}


	private List<QJoin> getJoinsBefore(QueryObject<?> queryObject, QJoin qjoin) {
		List<QJoin> result = new LinkedList<>();
		for (QJoin qj: queryObject.getJoins()) {
			if (qj != qjoin) {
				result.add(qj);
			}
			else {
				break;
			}
		}
		return result;
	}

}
