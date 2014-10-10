package scott.sort.test;

import javax.sql.DataSource;

import scott.sort.api.core.Environment;
import scott.sort.server.EntityContextServices;
import scott.sort.server.jdbc.persister.Persister;

public class TestEntityContextServices extends EntityContextServices {

    public interface PersisterFactory {
        public Persister newPersister(Environment env, String namespace);
    }

    private PersisterFactory fac;

    public TestEntityContextServices(DataSource dataSource) {
        super(dataSource);
    }

    public void setPersisterFactory(PersisterFactory fac) {
        this.fac = fac;
    }

    @Override
    protected Persister newPersister(Environment env, String namespace) {
        if (fac != null) {
            return fac.newPersister(env, namespace);
        }
        return super.newPersister(env, namespace);
    }

}
