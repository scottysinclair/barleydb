package com.smartstream.mi.model;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.exception.model.ProxyCreationException;

public class MiProxyFactory implements ProxyFactory {

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
