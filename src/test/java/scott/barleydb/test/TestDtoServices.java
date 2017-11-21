package scott.barleydb.test;

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

import org.example.acl.dto.AccessAreaDto;
import org.example.acl.dto.UserDto;
import org.example.etl.EtlServices;
import org.example.etl.dto.BusinessTypeDto;
import org.example.etl.dto.TemplateDto;
import org.example.etl.dto.XmlMappingDto;
import org.example.etl.dto.XmlStructureDto;
import org.example.etl.dto.XmlSyntaxModelDto;
import org.example.etl.model.SyntaxType;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import scott.barleydb.api.core.entity.Statistics;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;

public class TestDtoServices extends TestBase {

  private static final Logger LOG = LoggerFactory.getLogger(TestDtoServices.class);

  private EtlServices etlServices;

  private AccessAreaDto root;
  private UserDto scott;

  @Before
  public void init() {
    etlServices = new EtlServices(env, namespace);

    root = new AccessAreaDto();
    root.setName("root");
    root.getChildren().setFetched(false);

    scott = new UserDto();
    scott.setAccessArea(root);
    scott.setName("Scott");
    scott.setUuid("");
  }

  @Test
  public void testSaveXmlSyntax() throws SortServiceProviderException, SortPersistException, SortQueryException {

    long start = System.currentTimeMillis();

    XmlStructureDto structure = new XmlStructureDto();
    structure.setAccessArea(root);
    structure.setUuid("");
    structure.setName("MT940 -structure");


    XmlSyntaxModelDto syntax = new XmlSyntaxModelDto();
    syntax.setName("MT940");
    syntax.setAccessArea( root );
    syntax.setUser( scott );
    syntax.setSyntaxType(SyntaxType.ROOT);
    syntax.setUuid("");
    //TODO: doa test with fetched == false, and see that no deletes were performed.
    syntax.getMappings().setFetched(true);

    XmlMappingDto mapping = new XmlMappingDto();
    mapping.setXpath("/root");
    mapping.setTargetFieldName("name");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("/root2");
    mapping.setTargetFieldName("name2");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("/sub");
    mapping.setTargetFieldName("name3");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    XmlSyntaxModelDto subSyntax = new XmlSyntaxModelDto();
    mapping.setSubSyntax(subSyntax);
    subSyntax.setName("MT940 - sub");
    subSyntax.setAccessArea( root );
    subSyntax.setUser( scott );
    subSyntax.setSyntaxType(SyntaxType.SUBSYNTAX);
    subSyntax.setUuid("");
    subSyntax.getMappings().setFetched(true);

    mapping = new XmlMappingDto();
    mapping.setXpath("sub1");
    mapping.setTargetFieldName("name2");
    mapping.setSyntax(subSyntax);
    subSyntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("sub2");
    mapping.setTargetFieldName("name3");
    mapping.setSyntax(subSyntax);
    subSyntax.getMappings().add(mapping);

    XmlSyntaxModelDto subSubSyntax = new XmlSyntaxModelDto();
    mapping.setSubSyntax(subSubSyntax);
    subSubSyntax.setName("MT940 - sub - sub");
    subSubSyntax.setAccessArea( root );
    subSubSyntax.setUser( scott );
    subSubSyntax.setSyntaxType(SyntaxType.SUBSYNTAX);
    subSubSyntax.setUuid("");
    subSubSyntax.getMappings().setFetched(true);

    syntax.setStructure(structure);
    subSyntax.setStructure(structure);
    subSubSyntax.setStructure(structure);

    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax ----------------------------");
    etlServices.saveSyntax(syntax);
    System.out.println("Saved syntax " + syntax.getName() + " with ID " + syntax.getId());

    Statistics stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(0, stats.getNumberOfQueries());
    Assert.assertEquals(11, stats.getNumberOfRecordInserts());
    Assert.assertEquals(5, stats.getNumberOfBatchInserts());
    Assert.assertEquals(0, stats.getNumberOfRecordUpdates());
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());



    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax again ----------------------------");
    etlServices.saveSyntax(syntax);

    //is a NOOP
    stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(5, stats.getNumberOfQueries());
    Assert.assertEquals(5, stats.getNumberOfQueryDatabseCalls());
    Assert.assertEquals(0, stats.getNumberOfRecordInserts());
    Assert.assertEquals(0, stats.getNumberOfBatchInserts());
    Assert.assertEquals(0, stats.getNumberOfRecordUpdates());
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());


    syntax.setName("An improved MT940");
    mapping.setTargetFieldName("name3.1");

    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax again ----------------------------");
    etlServices.saveSyntax(syntax);

    //the syntax and subsyntax (OL) and subsyntax mapping records are all updated.
    stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(5, stats.getNumberOfQueries());
    Assert.assertEquals(5, stats.getNumberOfQueryDatabseCalls());
    Assert.assertEquals(0, stats.getNumberOfRecordInserts());
    Assert.assertEquals(0, stats.getNumberOfBatchInserts());
    Assert.assertEquals(3, stats.getNumberOfRecordUpdates());
    Assert.assertEquals(2, stats.getNumberOfBatchUpdates());
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());


    syntax = etlServices.loadFullXmlSyntax(syntax.getId());
    stats = etlServices.getLastCtxStatistics();
    //fetching the subsyntax and subsubsyntax
    Assert.assertEquals(3, stats.getNumberOfQueries());
    Assert.assertEquals(3, stats.getNumberOfQueryDatabseCalls());

    LOG.debug("Loaded syntax " + syntax.getId() + " with name " + syntax.getName());
    LOG.debug("Finished in " + (System.currentTimeMillis() - start) + " milli seconds");

    /*
     * test setting the DtoList to fetched == false
     */
    syntax.getMappings().setFetched(false);
    syntax.getMappings().clear();
    //as the mappings relation is not considered fetched, the cleared list will not cause any deletes
    etlServices.saveSyntax(syntax);
    syntax = etlServices.loadFullXmlSyntax(syntax.getId());
    Assert.assertEquals(3, syntax.getMappings().size()); //see everything is ok
    Assert.assertTrue(syntax.getMappings().isFetched()); //now that we reloaded, the mappings are fetched
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());

    syntax.getMappings().clear();
    //as the mappings relation is considered fetched, the cleared list will cause deletes
    etlServices.saveSyntax(syntax);
    stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(7, stats.getNumberOfRecordDeletes()); //the mappings + subyntaxes were all deleted
    Assert.assertEquals(2, stats.getNumberOfBatchDeletes()); //in 2 batches

    syntax = etlServices.loadFullXmlSyntax(syntax.getId());
    Assert.assertEquals(0, syntax.getMappings().size()); //see everything is deleted
    Assert.assertTrue(syntax.getMappings().isFetched()); //now that we reloaded, the mappings are fetched
    stats = etlServices.getLastCtxStatistics();
  }

  @Test
  public void testSaveTemplateDatatype() throws SortServiceProviderException, SortPersistException, SortQueryException {
    TemplateDto template = new TemplateDto();
    template.setAccessArea(root);
    template.setName("test-temp");
    template.getContents().setFetched(true);
    template.getBusinessTypes().setFetched(true);
    template.setUuid("");

    BusinessTypeDto bt1 = new BusinessTypeDto();
    bt1.setAccessArea(root);
    bt1.setName("bt1");
    bt1.setUuid("");

    BusinessTypeDto bt2 = new BusinessTypeDto();
    bt2.setAccessArea(root);
    bt2.setName("bt2");
    bt2.setUuid("");

    template.getBusinessTypes().add(bt1);
    template.getBusinessTypes().add(bt2);

    etlServices.saveTemplate(template);

    Statistics stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(0, stats.getNumberOfQueries());
    Assert.assertEquals(6, stats.getNumberOfRecordInserts());
    Assert.assertEquals(4, stats.getNumberOfBatchInserts());
    Assert.assertEquals(0, stats.getNumberOfRecordUpdates());
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());

    template.setName("test-temp-fix");
    template.getBusinessTypes().get(0).setName("bt1-fix");
    etlServices.saveTemplateAndBusinessTypes(template);

    stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(5, stats.getNumberOfQueries()); //queries loading original data for comparison
    Assert.assertEquals(5, stats.getNumberOfQueryDatabseCalls()); //queries loading original data for comparison
    Assert.assertEquals(0, stats.getNumberOfRecordInserts());
    Assert.assertEquals(2, stats.getNumberOfRecordUpdates()); //the template and busines type data
    Assert.assertEquals(2, stats.getNumberOfBatchUpdates());
    Assert.assertEquals(0, stats.getNumberOfRecordDeletes());

    template = etlServices.loadTemplate(template.getId());
    Assert.assertEquals(2, template.getBusinessTypes().size());

    stats = etlServices.getLastCtxStatistics();
    Assert.assertEquals(1, stats.getNumberOfQueries());
    Assert.assertEquals(1, stats.getNumberOfQueryDatabseCalls());

    //TODO:assert that the join table data was not affected by the resave.
  }

}
