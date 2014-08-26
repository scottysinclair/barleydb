package com.smartstream.morf.api.query;

import java.io.Serializable;

/**
 * A join in a query from one query object to another.
 * Contains the from and to side of the join as well as the field which is used in the join.
 * 
 * @author sinclair
 *
 */
public class QJoin implements Serializable {
	private static final long serialVersionUID = 1L;
	private final QueryObject<?> from;
	private final QueryObject<?> to;
	private final String fkeyProperty;
	public QJoin(QueryObject<?> from, QueryObject<?> to, String fkeyProperty) {
		this.from = from;
		this.to = to;
		this.fkeyProperty = fkeyProperty;
	}
	
	public QueryObject<?> getFrom() {
		return from;
	}

	public QueryObject<?> getTo() {
		return to;
	}
	
	public String getFkeyProperty() {
		return fkeyProperty;
	}

	@Override
	public String toString() {
		return "QJoin [from=" + from + ", fkeyProperty=" + fkeyProperty + ", to=" + to
				+ "]";
	}
}