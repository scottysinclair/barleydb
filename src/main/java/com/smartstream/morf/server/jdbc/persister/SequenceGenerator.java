package com.smartstream.morf.server.jdbc.persister;

import com.smartstream.morf.api.config.EntityType;

public interface SequenceGenerator {

    public Object getNextKey(EntityType entityType);

}
