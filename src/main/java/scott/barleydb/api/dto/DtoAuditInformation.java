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

import java.util.List;

import scott.barleydb.api.audit.AuditInformation;
import scott.barleydb.api.audit.AuditRecord;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.query.QProperty;

public class DtoAuditInformation {

  private final EntityContext ctx;
  private final DtoConverter converter;
  private AuditInformation auditInformation;

  public DtoAuditInformation(Environment env, String namespace) {
    this.ctx = new EntityContext(env, namespace);
    this.converter = new DtoConverter(env, namespace, ctx);
  }

  public void compareWithDatabase(List<? extends BaseDto> models) throws SortServiceProviderException, SortQueryException, SortPersistException {
    converter.importDtos(models);
    List<ProxyController> list = converter.getModels(models);
    auditInformation = ctx.compareWithDatabase(list);
  }

  public boolean hasChanges(BaseDto dto) {
    Entity entity = converter.getEntity(dto);
    AuditRecord record = auditInformation.getAuditRecord(entity);
    return record != null && record.hasChanges();
  }

  public AuditInformation getEntityAuditInformation() {
    return auditInformation;
  }

  /**
   *
   * @param dtoClass the DTO class
   * @param property the property of the DTO
   * @param value the value
   * @return true if a DTO object property was changed from or to the given value
   */
  public List<AuditRecord> getValueChangesMatching(Class<? extends BaseDto> dtoClass, QProperty<?> property, Object value) {
    EntityType et =  ctx.getDefinitions().getEntityTypeForDtoClass(dtoClass, true);
    return auditInformation.getValueChangesMatching(et, property, value);
  }

}
