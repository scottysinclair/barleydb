package scott.sort.server.jdbc.persister;

import scott.sort.api.config.EntityType;

public interface SequenceGenerator {

    public Object getNextKey(EntityType entityType);

}
