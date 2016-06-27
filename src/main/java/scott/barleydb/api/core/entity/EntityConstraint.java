package scott.barleydb.api.core.entity;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
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

import java.io.Serializable;

/**
 * Constraints on an entity
 *
 * @author scott
 *
 */
public class EntityConstraint implements Serializable {

    public static EntityConstraint mustExistInDatabase() {
        return new EntityConstraint(true, false);
    }

    public static EntityConstraint mustNotExistInDatabase() {
        return new EntityConstraint(false, true);
    }

    public static EntityConstraint noConstraints() {
        return new EntityConstraint(false, false);
    }

    private static final long serialVersionUID = 1L;

    private boolean mustExistInDatabase;

    private boolean mustNotExistInDatabase;

    public EntityConstraint(boolean mustExistInDatabase, boolean mustNotExistInDatabase) {
        this.mustExistInDatabase = mustExistInDatabase;
        this.mustNotExistInDatabase = mustNotExistInDatabase;
        if (mustExistInDatabase && mustNotExistInDatabase) {
            throw new IllegalArgumentException("Invalid constraint");
        }
    }

    public boolean isMustExistInDatabase() {
        return mustExistInDatabase;
    }

    public boolean isMustNotExistInDatabase() {
        return mustNotExistInDatabase;
    }

    public boolean noDatabaseExistenceConstraints() {
        return !mustExistInDatabase && !mustNotExistInDatabase;
    }

    public void setMustExistInDatabase() {
        this.mustExistInDatabase = true;
        this.mustNotExistInDatabase = false;
    }

    public void setMustNotExistInDatabase() {
        this.mustExistInDatabase = false;
        this.mustNotExistInDatabase = true;
    }

    public void set(EntityConstraint constraints) {
        this.mustExistInDatabase = constraints.mustExistInDatabase;
        this.mustNotExistInDatabase = constraints.mustNotExistInDatabase;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mustExistInDatabase ? 1231 : 1237);
        result = prime * result + (mustNotExistInDatabase ? 1231 : 1237);
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
        EntityConstraint other = (EntityConstraint) obj;
        if (mustExistInDatabase != other.mustExistInDatabase)
            return false;
        if (mustNotExistInDatabase != other.mustNotExistInDatabase)
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (mustExistInDatabase) {
            return "MustExist";
        }
        else if (mustNotExistInDatabase) {
            return "MustNotExist";
        }
        return "N/A";
    }

}
