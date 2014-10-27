package scott.sort.api.core.util;

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
