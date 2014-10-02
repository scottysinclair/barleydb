package scott.sort.api.query;

public class QLogicalOp extends QCondition {
    private static final long serialVersionUID = 1L;
    private final QCondition left;
    private final QCondition right;
    private final QBooleanOps expr;

    public QLogicalOp(QCondition left, QCondition right, QBooleanOps expr) {
        this.left = left;
        this.right = right;
        this.expr = expr;
    }

    public QCondition getLeft() {
        return left;
    }

    public QCondition getRight() {
        return right;
    }

    public QBooleanOps getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "QLogicalOp [left=" + left + ", expr=" + expr + ", right="
                + right + "]";
    }

    @Override
    public void visit(ConditionVisitor visitor) {
        visitor.visitLogicalOp(this);
    }
}