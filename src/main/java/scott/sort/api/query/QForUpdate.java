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

public class QForUpdate {

    private final Integer optionalWaitInSeconds;

    public QForUpdate(Integer optionalWaitInSeconds) {
        this.optionalWaitInSeconds = optionalWaitInSeconds;
    }

    public Integer getOptionalWaitInSeconds() {
        return optionalWaitInSeconds;
    }

}
