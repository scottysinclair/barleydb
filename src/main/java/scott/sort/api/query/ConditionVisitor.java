package scott.sort.api.query;

import scott.sort.api.exception.query.IllegalQueryStateException;

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
 * visitor interface to process a condition tree.
 * @author sinclair
 *
 */
public interface ConditionVisitor {
    public void visitPropertyCondition(QPropertyCondition qpc) throws IllegalQueryStateException;

    public void visitLogicalOp(QLogicalOp qlo) throws IllegalQueryStateException;

    public void visitExists(QExists exists) throws IllegalQueryStateException;
}