package com.smartstream.morf.api.query;

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