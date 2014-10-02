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
 * visitor interface to process a condition tree.
 * @author sinclair
 *
 */
public interface ConditionVisitor {
    public void visitPropertyCondition(QPropertyCondition qpc);

    public void visitLogicalOp(QLogicalOp qlo);

    public void visitExists(QExists exists);
}