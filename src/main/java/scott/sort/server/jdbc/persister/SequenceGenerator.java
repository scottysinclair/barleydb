package scott.sort.server.jdbc.persister;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.EntityType;
import scott.sort.server.jdbc.persister.exception.SortPersistException;

public interface SequenceGenerator {

    public Object getNextKey(EntityType entityType) throws SortPersistException;

}
