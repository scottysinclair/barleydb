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

import java.util.Arrays;
import java.util.List;

/**
 * There was a problem with choice elements
 * So here is a special test case for them
 */
public class XsdDefinitionChoiceTest extends ParentTest {

    XsdDefinition testXsd;

    @Before
    public void setup() throws Exception{
        testXsd = loadDefinition("schemas/loading/ChoiceExample.xsd");
    }

    @Test
    public void testStructureWithChoice() throws Exception {
        List<XsdElement> elements = testXsd.getChildTargetElements();
        assertElementNames(Arrays.asList("RateValues"), elements);

        List<XsdElement> children = elements.get(0).getChildTargetElements();

        assertElementNames(Arrays.asList("RateValueType", "RateSource", "ChoiceA", "BidRate", "AskRate", "MidRate", "ChoiceB", "ChoiceC"), children);

    }
}
