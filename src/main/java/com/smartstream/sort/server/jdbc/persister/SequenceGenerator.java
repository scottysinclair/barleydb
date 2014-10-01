package com.smartstream.sort.server.jdbc.persister;

import com.smartstream.sort.api.config.EntityType;

public interface SequenceGenerator {

    public Object getNextKey(EntityType entityType);

}
