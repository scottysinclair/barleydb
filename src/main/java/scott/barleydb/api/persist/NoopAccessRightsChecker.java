package scott.barleydb.api.persist;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;

/**
 * Implementation of AccessRightsChecker which just logs the fact that no access rights are checked.
 * @author scott
 *
 */
public class NoopAccessRightsChecker implements AccessRightsChecker {

  private static final Logger LOG = LoggerFactory.getLogger(NoopAccessRightsChecker.class);

  @Override
  public void verifyCreateRight(EntityContext ctx, Entity entity) {
    LOG.info("No access rights check for insert of {}", entity);
  }

  @Override
  public void verifyUpdateRight(EntityContext ctx, Entity entity) {
    LOG.info("No access rights check for update of {}", entity);
  }

  @Override
  public void verifyDeleteRight(EntityContext ctx, Entity entity) {
    LOG.info("No access rights check for deletion of {}", entity);
  }

}
