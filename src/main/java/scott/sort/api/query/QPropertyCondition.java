package scott.sort.api.query;

/**
 * A condition on a model property used to filter results.
 * @author sinclair
 *
 */
public class QPropertyCondition extends QCondition {
    private static final long serialVersionUID = 1L;
    private final QProperty<?> property;
    private final QMathOps operator;
    private final Object value;

    public QPropertyCondition(QProperty<?> property, QMathOps operator, Object value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public QProperty<?> getProperty() {
        return property;
    }

    public QMathOps getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void visit(ConditionVisitor visitor) {
        visitor.visitPropertyCondition(this);
    }

    @Override
    public String toString() {
        return "QPropertyCondition [property=" + property + ", operator="
                + operator + ", value=" + value + "]";
    }
}