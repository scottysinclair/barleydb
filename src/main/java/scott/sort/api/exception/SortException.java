package scott.sort.api.exception;

import java.util.UUID;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

public class SortException extends Exception {

    private static final long serialVersionUID = 1L;

    public SortException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public SortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortException(String message) {
        super(message);
    }

    public SortException(Throwable cause) {
        super(cause);
    }

    /**
     * Gets the corresponding entity in the given entity context.
     * @param entityContext
     * @param originalEntity
     * @return
     */
    protected Entity getCorrespondingEntity(EntityContext entityContext, Entity originalEntity) {
        UUID uuid = originalEntity.getUuid();
        Object key = originalEntity.getKey().getValue();
        Entity replacement = entityContext.getEntityByUuidOrKey(uuid, originalEntity.getEntityType(), key, false);
        return replacement != null ? replacement : originalEntity;
    }
}
