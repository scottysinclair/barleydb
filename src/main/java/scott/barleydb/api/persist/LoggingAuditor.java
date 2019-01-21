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

import scott.barleydb.api.audit.AuditInformation;
import scott.barleydb.api.audit.AuditRecord;
import scott.barleydb.api.audit.Change;

public class LoggingAuditor implements Auditor {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingAuditor.class.getName());

  @Override
  public void saveAuditInformation(AuditInformation audit) {
    for (AuditRecord auditRecord : audit.getRecords()) {
      for (Change change : auditRecord.changes()) {
        LOG.debug( auditRecord.formatChange(change) );
      }
      LOG.debug("------------------------------------------------------------");
  }
  }


}
