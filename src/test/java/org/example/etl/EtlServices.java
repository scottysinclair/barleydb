package org.example.etl;

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

import org.example.etl.dto.SyntaxModelDto;
import org.example.etl.dto.TemplateDto;
import org.example.etl.dto.XmlSyntaxModelDto;
import org.example.etl.model.BusinessType;
import org.example.etl.model.SyntaxModel;
import org.example.etl.model.Template;
import org.example.etl.model.XmlMapping;
import org.example.etl.model.XmlSyntaxModel;
import org.example.etl.query.QTemplate;
import org.example.etl.query.QXmlSyntaxModel;

import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.Statistics;
import scott.barleydb.api.dto.DtoConverter;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.persist.PersistRequest;

public class EtlServices {

  private final Environment env;
  private final String namespace;
  private Statistics lastCtxStatistics;

  public EtlServices(Environment env, String namespace) {
    this.env = env;
    this.namespace = namespace;
  }

  public void saveSyntax(SyntaxModelDto syntaxDto) throws SortServiceProviderException, SortPersistException, SortQueryException {
    EntityContext ctx = new EtlEntityContext(env);
    DtoConverter converter = new DtoConverter(env, namespace, ctx);
    converter.importDtos(syntaxDto);
    SyntaxModel syntax = converter.getModel(syntaxDto);
    ctx.persist(new PersistRequest().save(syntax));
    converter.convertToDtos();
    lastCtxStatistics = ctx.getStatistics();
  }

  public XmlSyntaxModelDto loadFullXmlSyntax(Long id) throws SortServiceProviderException, SortQueryException {
    EntityContext ctx = new EntityContext(env, namespace);

    QXmlSyntaxModel q = new QXmlSyntaxModel();
    q.joinToAccessArea();
    q.joinToUser();
    q.joinToStructure();
    q.joinToMappings();

    //register the syntax model query as a fetch plan so it will be used when lazily fetching a sub-syntax
    ctx.register(q);
    q.where(q.id().equal( id ));

    XmlSyntaxModel result = ctx.performQuery(q).getSingleResult();
    walkHierarchy(result);

    DtoConverter converter = new DtoConverter(env, namespace, ctx);
    converter.convertToDtos();
    lastCtxStatistics = ctx.getStatistics();
    return converter.getDto(result);
  }

  private void walkHierarchy(XmlSyntaxModel syntaxModel) throws SortServiceProviderException, SortQueryException {
    for (XmlMapping mapping: syntaxModel.getMappings()) {
       if (mapping.getSubSyntax() != null) {
         walkHierarchy(mapping.getSubSyntax());
       }
    }
  }

  public void saveTemplate(TemplateDto templateDto) throws SortServiceProviderException, SortPersistException, SortQueryException {
    save(templateDto, false);
  }

  public void saveTemplateAndBusinessTypes(TemplateDto templateDto) throws SortServiceProviderException, SortQueryException, SortPersistException {
    save(templateDto, true);
  }
  private void save(TemplateDto templateDto, boolean withBusinessTypes) throws SortServiceProviderException, SortQueryException, SortPersistException {
    EntityContext ctx = new EtlEntityContext(env);
    DtoConverter converter = new DtoConverter(env, namespace, ctx);
    converter.importDtos(templateDto);
    Template template = converter.getModel(templateDto);

    PersistRequest pr = new PersistRequest();
    pr.save(template);
    if (withBusinessTypes) {
      for (BusinessType bt: template.getBusinessTypes()) {
        pr.save(bt);
      }
    }
    ctx.persist(pr);
    converter.convertToDtos();
    lastCtxStatistics = ctx.getStatistics();
  }


  public TemplateDto loadTemplate(Long id) throws SortServiceProviderException, SortQueryException {
    EntityContext ctx = new EntityContext(env, namespace);

    QTemplate q = new QTemplate();
    q.joinToAccessArea();
    q.joinToBusinessType();
    q.where(q.id().equal(id));

    Template template = ctx.performQuery(q).getSingleResult();

    DtoConverter converter = new DtoConverter(env, namespace, ctx);
    converter.convertToDtos();
    lastCtxStatistics = ctx.getStatistics();
    return converter.getDto(template);
  }

  public Statistics getLastCtxStatistics() {
    return lastCtxStatistics;
  }

}
