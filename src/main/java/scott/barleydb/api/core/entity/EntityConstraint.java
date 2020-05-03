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
import java.util.Objects;

/**
 * Constraints on an entity
 *
 * @author scott
 *
 */
public class EntityConstraint implements Serializable {

    public static EntityConstraint mustExistInDatabase() {
        return new EntityConstraint(true, false, false, false);
    }

    public static EntityConstraint mustNotExistInDatabase() {
        return new EntityConstraint(false, true, false, false);
    }

    public static EntityConstraint noConstraints() {
        return new EntityConstraint(false, false, false, false);
    }

    public static EntityConstraint mustExistAndDontFetch() {
        return new EntityConstraint(true, false, true, false);
    }

    public static EntityConstraint dontFetch() {
        return new EntityConstraint(false, false, true, false);
    }

    public static EntityConstraint saveRequired() {
        return new EntityConstraint(false, false, false, true);
    }

    private static final long serialVersionUID = 1L;

    private boolean mustExistInDatabase;

    private boolean mustNotExistInDatabase;

    private boolean neverFetch;

    private boolean saveRequired;

    public EntityConstraint(boolean mustExistInDatabase, boolean mustNotExistInDatabase) {
        this(mustExistInDatabase, mustNotExistInDatabase, false, false);
    }
    public EntityConstraint(boolean mustExistInDatabase, boolean mustNotExistInDatabase, boolean neverFetch, boolean saveRequired) {
        this.mustExistInDatabase = mustExistInDatabase;
        this.mustNotExistInDatabase = mustNotExistInDatabase;
        if (mustExistInDatabase && mustNotExistInDatabase) {
            throw new IllegalArgumentException("Invalid constraint");
        }
        this.neverFetch = neverFetch;
        this.saveRequired = saveRequired;
    }

    public boolean isSaveRequired() {
        return saveRequired;
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

    public boolean isNeverFetch() {
        return neverFetch;
    }

    public void setSaveRequired(boolean value) {
        this.saveRequired = saveRequired;
    }

    public void set(EntityConstraint constraints) {
        this.mustExistInDatabase = constraints.mustExistInDatabase;
        this.mustNotExistInDatabase = constraints.mustNotExistInDatabase;
        this.neverFetch = constraints.neverFetch;
        this.saveRequired = constraints.saveRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityConstraint that = (EntityConstraint) o;
        return mustExistInDatabase == that.mustExistInDatabase &&
                mustNotExistInDatabase == that.mustNotExistInDatabase &&
                neverFetch == that.neverFetch &&
                saveRequired == that.saveRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mustExistInDatabase, mustNotExistInDatabase, neverFetch, saveRequired);
    }

    @Override
    public String toString() {
        if (mustExistInDatabase) {
            return  "MustExist" + (neverFetch ? " + NeverFetch" : "") ;
        }
        else if (mustNotExistInDatabase) {
            return "MustNotExist" + (neverFetch ? " + NeverFetch" : "");
        }
        return "N/A" + (neverFetch ? " + NeverFetch" : "");
    }

}
