package scott.sort.api.core;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.DefinitionsSet;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.proxy.EntityProxy;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.persister.SequenceGenerator;
import scott.sort.server.jdbc.queryexecution.QueryPreProcessor;

/**
 * Exists on both the client and the server
 *
 * @author scott
 *
 */
public final class Environment {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(Environment.class);

    private final DefinitionsSet definitionsSet;

    private QueryPreProcessor queryPreProcessor;

    private final IEntityContextServices entityContextServices;

    private SequenceGenerator sequenceGenerator;

    private static final ThreadLocal<Map<String, Object>> resources = new ThreadLocal<Map<String, Object>>() {
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    public void preProcess(QueryObject<?> query, Definitions definitions) {
        if (queryPreProcessor != null) {
            queryPreProcessor.preProcess(query, definitions);
        }
    }

    public void setQueryPreProcessor(QueryPreProcessor queryPreProcessor) {
        this.queryPreProcessor = queryPreProcessor;
    }

    public Environment(IEntityContextServices entityContextServices) {
        this.entityContextServices = entityContextServices;
        this.definitionsSet = new DefinitionsSet();
    }

    public Object getThreadLocalResource(String key, boolean required) {
        Object o = resources.get().get(key);
        if (o == null && required) {
            throw new IllegalStateException("Resource '" + key + "' is required.");
        }
        return o;
    }

    public SequenceGenerator getSequenceGenerator() {
        return sequenceGenerator;
    }

    public void setSequenceGenerator(SequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    public void setThreadLocalResource(String key, Object value) {
        resources.get().put(key, value);
    }

    public void clearThreadLocalResource(String key) {
        resources.get().remove(key);
    }

    public void clearThreadLocalResources() {
        resources.remove();
    }

    /**
     *
     * @param namespace
     * @return
     * @throws IllegalStateException if not found
     */
    public Definitions getDefinitions(String namespace) {
        return definitionsSet.getDefinitions(namespace);
    }

    public void addDefinitions(Definitions definitions) {
        definitionsSet.addDefinitions(definitions);
    }

    public IEntityContextServices services() {
        return entityContextServices;
    }

    public <T> T generateProxy(Entity entity) throws ClassNotFoundException {
        //LOG.debug("generateProxy for " + entity);
        for (ProxyFactory fac : entity.getEntityContext().getDefinitions().getProxyFactories()) {
            if (fac != null) {
                T proxy = fac.newProxy(entity);
                if (proxy != null) {
                    return proxy;
                }
            }
        }
        return EntityProxy.generateProxy(entity.getEntityContext().getDefinitions().getProxyClassLoader(), entity);
    }

}
