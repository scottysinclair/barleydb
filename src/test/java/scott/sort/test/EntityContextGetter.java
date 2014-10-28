package scott.sort.test;

import scott.sort.api.core.entity.EntityContext;

public class EntityContextGetter {
    private final boolean client;
    public EntityContextGetter(boolean client) {
        this.client = client;
    }

    public EntityContext get(TestRemoteClientBase thisTest) {
        if (client) {
            return thisTest.clientEntityContext;
        }
        else {
            return thisTest.serverEntityContext;
        }
    }

}
