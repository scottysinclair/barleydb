package org.example.etl.model;

/*
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
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.exception.model.ProxyCreationException;

public class EtlProxyFactory implements ProxyFactory {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public <T> T newProxy(Entity entity) throws ProxyCreationException {
    if (entity.getEntityType().getInterfaceName().equals(SyntaxModel.class.getName())) {
      return (T) new SyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(XmlSyntaxModel.class.getName())) {
      return (T) new XmlSyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(XmlStructure.class.getName())) {
      return (T) new XmlStructure(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(XmlMapping.class.getName())) {
      return (T) new XmlMapping(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CsvSyntaxModel.class.getName())) {
      return (T) new CsvSyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CsvStructure.class.getName())) {
      return (T) new CsvStructure(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CsvStructureField.class.getName())) {
      return (T) new CsvStructureField(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CsvMapping.class.getName())) {
      return (T) new CsvMapping(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(Template.class.getName())) {
      return (T) new Template(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(TemplateContent.class.getName())) {
      return (T) new TemplateContent(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(BusinessType.class.getName())) {
      return (T) new BusinessType(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(TemplateBusinessType.class.getName())) {
      return (T) new TemplateBusinessType(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(RawData.class.getName())) {
      return (T) new RawData(entity);
    }
    return null;
  }
}
