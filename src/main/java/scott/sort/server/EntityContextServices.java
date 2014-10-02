package scott.sort.server;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import scott.sort.api.config.Definitions;
import scott.sort.api.core.Environment;
import scott.sort.api.core.IEntityContextServices;
import scott.sort.api.core.QueryBatcher;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.persister.PersistAnalyser;
import scott.sort.server.jdbc.persister.Persister;
import scott.sort.server.jdbc.queryexecution.QueryExecuter;
import scott.sort.server.jdbc.queryexecution.QueryExecution;
import scott.sort.server.jdbc.queryexecution.QueryResult;

/**
 * Server implementation of entity context services
 * @author scott
 *
 */
public class EntityContextServices implements IEntityContextServices {

    private Environment env;

    private final DataSource dataSource;

    public EntityContextServices(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setEnvironment(Environment env) {
        this.env = env;
    }

    @Override
    public Definitions getDefinitions(String namespace) {
        return env.getDefinitions(namespace);
    }

    @Override
    public <T> QueryResult<T> execute(String namespace, EntityContext entityContext, QueryObject<T> query) throws Exception {
        env.preProcess(query, entityContext.getDefinitions());
        QueryExecution<T> execution = new QueryExecution<T>(entityContext, query, env.getDefinitions(namespace));

        try (MyClosable<Connection> closableConnection = getConnection();) {
            QueryExecuter executer = new QueryExecuter(closableConnection.getObject(), entityContext);
            executer.execute(execution);
            return execution.getResult();
        }
    }

    @Override
    public QueryBatcher execute(String namespace, EntityContext entityContext, QueryBatcher queryBatcher) throws Exception {
        QueryExecution<?> queryExecutions[] = new QueryExecution[queryBatcher.size()];
        int i = 0;
        for (QueryObject<?> queryObject : queryBatcher.getQueries()) {
            env.preProcess(queryObject, entityContext.getDefinitions());
            queryExecutions[i++] = new QueryExecution<>(entityContext, queryObject, env.getDefinitions(namespace));
        }

        try (MyClosable<Connection> closableConnection = getConnection();) {
            QueryExecuter exec = new QueryExecuter(closableConnection.getObject(), entityContext);
            exec.execute(queryExecutions);
            for (i = 0; i < queryExecutions.length; i++) {
                queryBatcher.addResult(queryExecutions[i].getResult());
            }
            return queryBatcher;
        }
    }

    @Override
    public PersistAnalyser execute(PersistAnalyser persistAnalyser) throws Exception {
        Persister persister = new Persister(env, persistAnalyser.getEntityContext().getNamespace());

        try (MyClosable<Connection> closableConnection = getConnection();) {
            env.setThreadLocalResource(Connection.class.getName(), closableConnection.getObject());
            persister.persist(persistAnalyser);
            return persistAnalyser;
        }
    }

    private MyClosable<Connection> getConnection() throws SQLException {
        Connection connection = (Connection) env.getThreadLocalResource(Connection.class.getName(), false);
        if (connection != null) {
            return new MyClosable<Connection>(connection, false);
        }
        connection = dataSource.getConnection();
        /*
         * Set the new connection on the thread local.
         */
        env.setThreadLocalResource(Connection.class.getName(), connection);
        return new MyClosable<Connection>(connection, true) {
            @Override
            protected void afterReallyClose() {
                /*
                 * release the thread local when the connection is closed.
                 */
                env.clearThreadLocalResource(Connection.class.getName());
            }
        };
    }

}

class MyClosable<T extends AutoCloseable> implements AutoCloseable {
    private final T closable;
    private boolean reallyClose;

    public MyClosable(T closable, boolean reallyClose) {
        this.closable = closable;
        this.reallyClose = reallyClose;
    }

    @Override
    public void close() throws Exception {
        if (reallyClose) {
            try {
                closable.close();
            } finally {
                afterReallyClose();
            }
        }
    }

    protected void afterReallyClose() {}

    public T getObject() {
        return closable;
    }

}
