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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class SwiftTest extends ParentTest {

    public static final String importPath = "schemas/";
    private static XsdDefinition mt940;
    private static XsdDefinition mtmsg;

    @Before
    public void before() throws Exception {
        mtmsg = loadDefinition(importPath + "mtmsg.2011.xsd");
        mt940 = loadDefinition(importPath + "fin.940.2011.xsd");
    }


    @Test
    public void testMt940Structure() throws Exception {
        XsdType finMessageComplexType = mtmsg.findType(new QualifiedName("urn:swift:xsd:mtmsg.2011", "FinMessage_Type"));
        assertNotNull(finMessageComplexType);
        XsdElement block4 = finMessageComplexType.getElement(new QualifiedName("urn:swift:xsd:mtmsg.2011", "Block4"));
        assertNotNull(block4);

        XsdElement document = mt940.findElement(new QualifiedName("urn:swift:xsd:fin.940.2011", "Document"));
        assertNotNull(document);

        /*
         * insert the fin 940 document element as the content of the Block4 any element.
         */
        XsdComplexType ctype = (XsdComplexType) block4.getType();
        XsdAny any = ctype.getComplexContent().getExtension().getSequence().getAny();
        any.setContent(document);

        assertEquals(1, block4.getChildTargetElements().size());
        assertSame(document.getDomElement(), block4.getChildTargetElements().get(0).getDomElement());
    }

}
