package scott.sort.api.query;

public class QOrderBy {

    private final QProperty<?> property;

    private final boolean ascending;

    public QOrderBy(QProperty<?> property, boolean ascending) {
        this.property = property;
        this.ascending = ascending;
    }

    public QProperty<?> getProperty() {
        return property;
    }

    public boolean isAscending() {
        return ascending;
    }

}
