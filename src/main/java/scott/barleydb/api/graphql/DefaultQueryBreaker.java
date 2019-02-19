package scott.barleydb.api.graphql;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
 *       <scottysinclair@gmail.com>
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
	private final int breakBadNestingEvery;
	private final int maximumDepth;
	/**
	 * types which we force a break when joining to
	 */
	private final Set<String> forceBreakSet;
	
	public DefaultQueryBreaker(Environment env, String namespace, int breakBadNestingEvery, int maximumDepth) {
		this(env, namespace, breakBadNestingEvery, maximumDepth, null);
	}
	public DefaultQueryBreaker(Environment env, String namespace, int breakBadNestingEvery, int maximumDepth, Collection<String> forceBreak) {
		this.env = env;
		this.namespace = namespace;
		this.breakBadNestingEvery = breakBadNestingEvery;
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
        List<QJoin> allJoins = getAllJoins(queryObject, true, new LinkedList<>());
		List<QJoin> all1toNJoins = getOneToManyJoins(queryObject, true, new LinkedList<>());
        result.addAll( breakAllJoinsWhichAreForced(all1toNJoins) );
		result.addAll( breakOneToManyJoinsWhichShouldntNest(allJoins) );
		result.addAll( breakAllJoinsWhichAreTooDeeplyNested(all1toNJoins, result));
		return result;
	}

    private Set<QJoin> breakAllJoinsWhichAreForced(List<QJoin> all1toNJoins) {
        Set<QJoin> result = new HashSet<>();
        for (QJoin qj: all1toNJoins) {
            if (isOneToMany(qj) && forceBreakSet.contains(qj.getTo().getTypeName())) {
                result.add(qj);
            }
        }
        return result;
    }


    private Set<QJoin> breakOneToManyJoinsWhichShouldntNest(List<QJoin> allJoinsInTheQuery) {
        Set<QJoin> result = new HashSet<>();
	    QJoin prev = null;
	    int counter = 0;
        for (QJoin qj: allJoinsInTheQuery) {
            if (prev != null && isOneToMany(prev) && prev.getTo() != qj.getFrom()) {
		        if (++counter >= breakBadNestingEvery) {
          			result.add(qj);
          			counter = 0;
				}
            }
            prev = qj;
        }
        return result;
    }

    private Set<QJoin> breakAllJoinsWhichAreTooDeeplyNested(List<QJoin> all1toNJoins, Set<QJoin> breaks) {
        Set<QJoin> result = new HashSet<>();
        int counter = 0;
        for (QJoin qj: all1toNJoins) {
            if (breaks.contains(qj)) {
                counter = 0;
            }
            else {
                counter++;
            }
            if (counter > maximumDepth) {
                result.add(qj);
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

    /**
     * @return all joins in order of evaluation.
     */
    private List<QJoin> getAllJoins(QueryObject<?> queryObject, boolean includeNested, List<QJoin> result) {
        for (QJoin qj: queryObject.getJoins()) {
             result.add(qj);
            if (includeNested) {
                getAllJoins(qj.getTo() ,  true, result);
            }
        }
        return result;
    }

    /**
     * @return all 1:N joins in order of evaluation.
     */
	private List<QJoin> getOneToManyJoins(QueryObject<?> queryObject, boolean includeNested, List<QJoin> result) {
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
