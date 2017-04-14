package scott.barleydb.api.persist;

import java.io.Serializable;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import scott.barleydb.api.core.entity.Entity;

public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Entity entity;
    public OperationType opType;

    public Operation(Entity entity, OperationType opType) {
        this.entity = entity;
        this.opType = opType;
    }

    public boolean isDelete() {
        return opType == OperationType.DELETE;
    }

    @Override
    public String toString() {
        return "Operation [entity=" + entity + ", opType=" + opType + "]";
    }

    public boolean isInsert() {
        return opType == OperationType.INSERT;
    }

    public boolean isUpdate() {
        return opType == OperationType.UPDATE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entity == null) ? 0 : entity.hashCode());
        result = prime * result + ((opType == null) ? 0 : opType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Operation other = (Operation) obj;
        if (entity == null) {
            if (other.entity != null)
                return false;
        } else if (!entity.equals(other.entity))
            return false;
        if (opType != other.opType)
            return false;
        return true;
    }

    public boolean isNone() {
        return opType == OperationType.NONE;
    }

    public void updateOpType(OperationType opType) {
        this.opType = opType;
    }

}
