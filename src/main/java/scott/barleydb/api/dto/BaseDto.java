package scott.barleydb.api.dto;

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

import java.io.Serializable;
import java.util.UUID;

import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityState;

public class BaseDto implements Serializable {

  private static final long serialVersionUID = 1L;
 /**
   * UUID for housekeeping.
   */
  private UUID uuid = UUID.randomUUID();
  /**
   * entity constraints
   */
  private EntityConstraint constraints = EntityConstraint.noConstraints();

  private EntityState entityState = EntityState.NOTLOADED;

  public EntityConstraint getConstraints() {
    return constraints;
  }

  public void setConstraints(EntityConstraint constraints) {
    this.constraints = constraints;
  }

  public UUID getBaseDtoUuid() {
    return uuid;
  }

  public String getBaseDtoUuidFirst7() {
    return uuid.toString().substring(0, 7);
  }

  public void setBaseDtoUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public EntityState getEntityState() {
    return entityState;
  }

  public void setEntityState(EntityState entityState) {
    this.entityState = entityState;
  }

}
