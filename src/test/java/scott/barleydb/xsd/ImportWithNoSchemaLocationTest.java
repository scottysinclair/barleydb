package scott.barleydb.xsd;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.MissingSchemaInfo.DependencyType;
import scott.barleydb.xsd.exception.SchemaNotFoundException;
import scott.barleydb.xsd.exception.XsdDefinitionException;

public class ImportWithNoSchemaLocationTest extends ParentTest {

    private static final Logger LOG = LoggerFactory.getLogger(ImportWithNoSchemaLocationTest.class);

    private XsdDefinition schemaImporting;

    public static final String importPath = "schemas/loading/import_with_no_schema_loc";
    @Before
    public void before() throws Exception {
        schemaImporting = loadDefinition(importPath + "/schema_importing_nosl.xsd");
    }

    public static XsdDefinition loadDefinition(String path) throws Exception {
        XsdDefinition xsdDefinition = new XsdDefinition(path, load(path), null, false);
        xsdDefinition.setDefinitionResolver(new MyTestXsdDefinitionResolver());
        xsdDefinition.resolveIncludesAndImports();
        return xsdDefinition;
    }

    @Test
    public void test() throws XsdDefinitionException {
        List<XsdElement> elementList = schemaImporting.getChildTargetElements();
        assertEquals("One possible root element in XSD", 1, elementList.size());
        XsdElement root = elementList.get(0);
        assertThat(root.getElementName(), equalTo("root"));
        assertThat(root.getNamespaceUri(), equalTo("urn:a"));

        elementList = root.getChildTargetElements();
        assertEquals("root element has only one child", 3, elementList.size());
        XsdElement childa = elementList.get(0);
        assertThat(childa.getElementName(), equalTo("childa"));
        assertThat(childa.getNamespaceUri(), equalTo("urn:b"));

        XsdElement childb = elementList.get(1);
        assertThat(childb.getElementName(), equalTo("childb"));
        assertThat(childb.getNamespaceUri(), equalTo("urn:b"));

        XsdElement childc = elementList.get(2);
        assertThat(childc.getElementName(), equalTo("childc"));
        assertThat(childc.getNamespaceUri(), nullValue());
    }

    public static class MyTestXsdDefinitionResolver extends TestXsdDefinitionResolver {

        @Override
        public Set<XsdDefinition> resolveImport(XsdDefinition forDef, String schemaLocation, String namespace) throws SchemaNotFoundException, InvalidXsdException {
            Set<XsdDefinition> result = new HashSet<>();
            try {
                if (namespace == null) {
                    byte data[] = load(importPath + "/schema_imported_nosl_childc.xsd");
                    XsdDefinition xsdDefinition = new XsdDefinition("schema_imported_nosl_childc.xsd", data, forDef, false);
                    result.add(xsdDefinition);
                    return result;
                }
                if (namespace.equals("urn:b")) {
                    byte data[] = load(importPath + "/schema_imported_nosl_childa.xsd");
                    XsdDefinition xsdDefinition = new XsdDefinition("schema_imported_nosl_childa.xsd", data, forDef, false);
                    result.add(xsdDefinition);
                    data = load(importPath + "/schema_imported_nosl_childb.xsd");
                    xsdDefinition = new XsdDefinition("schema_imported_nosl_childb.xsd", data, forDef, false);
                    result.add(xsdDefinition);
                    return result;
                }
            }
            catch(Exception x) {
                LOG.error("Caught exception resolving test schema", x);
                throw new InvalidXsdException("Unexpected error resolving test schema", x);
            }
            throw new SchemaNotFoundException(DependencyType.IMPORT, namespace, null);
        }

    }
}
