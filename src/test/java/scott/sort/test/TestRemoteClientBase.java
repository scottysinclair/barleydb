package scott.sort.test;

import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.RuntimeProperties;
import scott.sort.api.query.RuntimeProperties.Concurrency;
import scott.sort.api.query.RuntimeProperties.ScrollType;

import com.smartstream.mi.MiEntityContext;

/**
 * Tests based on a remote client environment
 * where auto commit must be true and all data passed into and back from
 * the entity context services gets serialized.
 *
 *
 * @author scott
 *
 */
public class TestRemoteClientBase extends TestBase {

    private static Environment clientEnv;
    private static RemoteClientEntityContextServices clientEntityContextServices;
    protected EntityContext clientEntityContext;

    @Override
    public void setup() throws Exception {
        super.setup();

        if (clientEnv == null) {
            clientEntityContextServices = new RemoteClientEntityContextServices(entityContextServices);
            clientEnv = new Environment(clientEntityContextServices);
            /*
             * We need to set the server environment for the thread local handling
             * during de-serialization for the objects reaching the server
             */
            clientEntityContextServices.setServerEnvironment( env );
            clientEntityContextServices.setClientEnvironment( clientEnv );
            clientEnv.loadDefinitions();

            /*
             * The client executes by default in the same context
             * The client is anyway remote, so the entity context is always copied
             * So setting executeInSameContext to false would cause double copying.
             *
             * Provides reasonable values for result-set scrolling and fetching
             */
            clientEnv.setDefaultRuntimeProperties(
                    new RuntimeProperties()
                        .concurrency(Concurrency.READ_ONLY)
                        .fetchSize(100)
                        .executeInSameContext(true)
                        .scrollType(ScrollType.FORWARD_ONLY));

        }
        clientEntityContext = new MiEntityContext(clientEnv);
    }


}
