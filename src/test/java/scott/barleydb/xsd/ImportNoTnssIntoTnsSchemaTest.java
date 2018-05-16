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

import org.junit.Before;
import org.junit.Test;

import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.List;

/**
 *
 * Test importing a schema with no target namespace into a schema with a namespace
 *
 */
public class ImportNoTnssIntoTnsSchemaTest extends  ParentTest {

    private XsdDefinition schemaImporting;

    public static final String importPath = "schemas/loading/import_no_tns_into_tns_schema";
    @Before
    public void before() throws Exception {
        schemaImporting = loadDefinition(importPath + "/schema_importing.xsd");
    }

    @Test
    public void testSchemaStructure() throws Exception {
        List<XsdElement> elementList = schemaImporting.getChildTargetElements();
        assertEquals("One possible root element in XSD", 1, elementList.size());
        XsdElement root = elementList.get(0);
        assertThat(root.getElementName(), equalTo("root"));
        assertThat(root.getNamespaceUri(), equalTo("urn:a"));

        elementList = root.getChildTargetElements();
        assertEquals("root element has only one child", 1, elementList.size());
        XsdElement childa = elementList.get(0);
        assertThat(childa.getElementName(), equalTo("childa"));
        assertThat(childa.getNamespaceUri(), nullValue());
    }



}

