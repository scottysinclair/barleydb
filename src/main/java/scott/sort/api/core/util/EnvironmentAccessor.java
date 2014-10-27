package scott.sort.api.core.util;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.core.Environment;

public class EnvironmentAccessor {

    private static ThreadLocal<Environment> environments = new ThreadLocal<Environment>();

    public static Environment get() {
        return environments.get();
    }

    public static void set(Environment environment) {
        environments.set(environment);
    }

    public static void remove() {
        environments.remove();
    }

}
