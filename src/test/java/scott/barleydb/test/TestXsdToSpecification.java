package scott.barleydb.test;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Before;
import org.junit.Test;

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specgen.fromxsd.FromXsdSchemaToSpecification;
import scott.barleydb.build.specification.modelgen.GenerateDtoModels;
import scott.barleydb.build.specification.modelgen.GenerateEnums;
import scott.barleydb.build.specification.modelgen.GenerateProxyModels;
import scott.barleydb.build.specification.modelgen.GenerateQueryModels;
import scott.barleydb.xsd.ParentTest;
import scott.barleydb.xsd.XsdDefinition;

public class TestXsdToSpecification {

  public static final String importPath = "schemas/fpml";
  private static XsdDefinition tradeCapture;

  @Before
  public void before() throws Exception {
      tradeCapture = ParentTest.loadDefinition(importPath + "/fixml-tradecapture-impl-5-0-SP2.xsd");
  }


  @Test
  public void testGenerateFpmlSpec() throws Exception {
    FromXsdSchemaToSpecification gen = new FromXsdSchemaToSpecification("org.fpml");
    SpecRegistry registry = gen.generateSpecification(tradeCapture);
    DefinitionsSpec fpmlSpec = registry.getDefinitionsSpec("org.fpml");

    GenerateEnums generateEnums = new GenerateEnums();
    generateEnums.generateEnums("target", fpmlSpec);

    GenerateProxyModels generateDataModels = new GenerateProxyModels();
    generateDataModels.generateDataModels("target", fpmlSpec);

    GenerateQueryModels generateQueryModels = new GenerateQueryModels();
    generateQueryModels.generateQueryModels("target", fpmlSpec);

    GenerateDtoModels generateDtoModels = new GenerateDtoModels();
    generateDtoModels.generateDtoModels("target", fpmlSpec);

    byte xml[] = toXml(registry);
    try ( FileOutputStream fout = new FileOutputStream(new File("target/fpml-spec.xml")); ) {
      fout.write(xml);
      fout.flush();
    }

  }

  private byte[] toXml(SpecRegistry registry ) throws Exception {
    JAXBContext jc = JAXBContext.newInstance(SpecRegistry.class);
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    marshaller.marshal(registry, System.out);

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    marshaller.marshal(registry, bout);
    return bout.toByteArray();
  }

}
