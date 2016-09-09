package org.example.etl.model;
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
    if (entity.getEntityType().getInterfaceName().equals(CSyntaxModel.class.getName())) {
      return (T) new CSyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CXmlSyntaxModel.class.getName())) {
      return (T) new CXmlSyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CXmlStructure.class.getName())) {
      return (T) new CXmlStructure(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CXmlMapping.class.getName())) {
      return (T) new CXmlMapping(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CCsvSyntaxModel.class.getName())) {
      return (T) new CCsvSyntaxModel(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CCsvStructure.class.getName())) {
      return (T) new CCsvStructure(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CCsvStructureField.class.getName())) {
      return (T) new CCsvStructureField(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CCsvMapping.class.getName())) {
      return (T) new CCsvMapping(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CTemplate.class.getName())) {
      return (T) new CTemplate(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CTemplateContent.class.getName())) {
      return (T) new CTemplateContent(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CBusinessType.class.getName())) {
      return (T) new CBusinessType(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CTemplateBusinessType.class.getName())) {
      return (T) new CTemplateBusinessType(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(CRawData.class.getName())) {
      return (T) new CRawData(entity);
    }
    return null;
  }
}
