package scott.sort.api.query;

public class QForUpdate {

    private final Integer optionalWaitInSeconds;

    public QForUpdate(Integer optionalWaitInSeconds) {
        this.optionalWaitInSeconds = optionalWaitInSeconds;
    }

    public Integer getOptionalWaitInSeconds() {
        return optionalWaitInSeconds;
    }

}
