package scott.sort.api.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


/**
 * query condition which checks for the existence of sub-query results.
 * @author sinclair
 *
 */
public class QExists extends QCondition {

    private static final long serialVersionUID = 1L;
    private final QueryObject<?> subQueryObject;

    public QExists(QueryObject<?> subQueryObject) {
        this.subQueryObject = subQueryObject;
    }

    public QueryObject<?> getSubQueryObject() {
        return subQueryObject;
    }

    @Override
    public void visit(ConditionVisitor visitor) {
        visitor.visitExists(this);
    }

}
