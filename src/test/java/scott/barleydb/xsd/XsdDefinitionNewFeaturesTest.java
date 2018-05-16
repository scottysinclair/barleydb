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

import scott.barleydb.xsd.exception.SchemaNotFoundException;
import scott.barleydb.xsd.XsdAttribute;
import scott.barleydb.xsd.XsdDefinition;
import scott.barleydb.xsd.XsdElement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Test cases brought up during implementation
 * which are not covered in the existing test schemas
 */
public class XsdDefinitionNewFeaturesTest extends ParentTest {

    XsdDefinition testXsdA;
    XsdDefinition testXsdB;

    @Before
    public void before() throws Exception {
        testXsdA = loadDefinition("schemas/loading/TestXsdA.xsd");
        testXsdB = loadDefinition("schemas/loading/TestXsdB.xsd");
    }

    @Test
    public void testMissingImportsAndIncludes() throws Exception {
        try {
            loadDefinition("schemas/loading/TestMissingIncludesAndImportsTopLevel.xsd");
        }
        catch(SchemaNotFoundException x) {
            assertEquals(6, x.getMissingSchemas().size());
            assertMissingSchemaLocations(Arrays.asList("schemas/loading/not-to-be-found-b.xsd",
                    "schemas/loading/not-to-be-found-f.xsd",
                    "schemas/loading/not-to-be-found-d.xsd", null, null, null), x.getMissingSchemas());
            assertMissingNamespaceUris(Arrays.asList("urn:not-to-be-found-a", "urn:not-to-be-found-e", "urn:not-to-be-found-c", null, null, null), x.getMissingSchemas());
        }
    }

    @Test
    public void testRootElements() throws Exception {
        assertEquals(14, testXsdA.getChildTargetElements().size());
    }

    @Test
    public void testElementWithComplexTypeFromOtherNamespace() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("person", element.get(0).getElementName());
        List<XsdElement> personElements = element.get(0).getChildTargetElements();
        assertElementNames(Arrays.asList("firstName", "lastName", "age"), personElements);
    }

    @Test
    public void testElementWithComplexContentRestictionWithComplexTypeFromOtherNamespace() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("personRestricted", element.get(1).getElementName());
        List<XsdElement> personElements = element.get(1).getChildTargetElements();
        assertElementNames(Arrays.asList("firstName", "lastName"), personElements);
    }

    @Test
    public void testElementWithComplexContentExtensionWithComplexTypeFromOtherNamespace() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("personRestrictedThenExtended", element.get(2).getElementName());
        List<XsdElement> personElements = element.get(2).getChildTargetElements();
        assertElementNames(Arrays.asList("firstName", "lastName", "sex", "height"), personElements);
    }

    @Test
    public void testComplexTypeWithSequence() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("elementWithSequence", element.get(3).getElementName());
        List<XsdElement> sequenceElements = element.get(3).getChildTargetElements();
        assertElementNames(Arrays.asList("seq_a", "seq_b", "seq_c"), sequenceElements);
    }

    @Test
    public void testComplexTypeWithChoice() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("elementWithChoice", element.get(4).getElementName());
        List<XsdElement> choiceElements = element.get(4).getChildTargetElements();
        assertElementNames(Arrays.asList("choice_a", "choice_b", "choice_c"), choiceElements);
    }

    @Test
    public void testComplexTypeWithAll() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("elementWithAll", element.get(5).getElementName());
        List<XsdElement> allElements = element.get(5).getChildTargetElements();
        assertElementNames(Arrays.asList("all_a", "all_b", "all_c"), allElements);
    }

    @Test
    public void testComplexTypeWithGroup() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("order", element.get(6).getElementName());
        List<XsdElement> orderElements = element.get(6).getChildTargetElements();
        assertElementNames(Arrays.asList("customer", "orderdetails", "shipto", "billto"), orderElements);
    }

    @Test
    public void testElementRef() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("happyMealRefExample", element.get(7).getElementName());
        List<XsdElement> happyMealElements = element.get(7).getChildTargetElements();
        assertElementNames(Arrays.asList("burger", "chips", "coke"), happyMealElements);
        XsdElement burger = happyMealElements.get(0);
        assertElementNames(Arrays.asList("extraFat"), burger.getChildTargetElements());
    }

    /**
     * Any doesn't impact the structure
     * since it doesn't tell us anything about child elements
     * @throws Exception
     */
    @Test
    public void testComplexTypeWithAny() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        assertEquals("elementWithAny", element.get(8).getElementName());
        List<XsdElement> elements = element.get(8).getChildTargetElements();
        assertElementNames(Arrays.asList("seq_a", "seq_b"), elements);
    }

    @Test
    public void testComplexElementWithAttributesAndAttributeGroup() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement complexElementWithAttributesAndAttributeGroup =  element.get(9);
        assertEquals("complexElementWithAttributesAndAttributeGroup", complexElementWithAttributesAndAttributeGroup.getElementName());

        //assert that we can access the child element which is part of the complex type
        assertEquals(1, complexElementWithAttributesAndAttributeGroup.getChildTargetElements().size());
        assertEquals("childElement", complexElementWithAttributesAndAttributeGroup.getChildTargetElements().get(0).getElementName());

        //assert that we find all of the attributes
        List<XsdAttribute> attributes = complexElementWithAttributesAndAttributeGroup.getAttributes();
        assertAttributeNames(Arrays.asList("attr1", "attrGroup_attr1", "attrGroup_attrRef3", "refedAttr2"), attributes);
    }

    @Test
    public void testElementWithSimpleContentAndExtensionAttribute() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement elementWithSimpleContentAndExtensionAttribute =  element.get(10);
        assertEquals("elementWithSimpleContentAndExtensionAttribute", elementWithSimpleContentAndExtensionAttribute.getElementName());

        //assert that there are no child elements
        assertTrue(elementWithSimpleContentAndExtensionAttribute.getChildTargetElements().isEmpty());

        //assert that we find all of the attributes
        List<XsdAttribute> attributes = elementWithSimpleContentAndExtensionAttribute.getAttributes();
        assertAttributeNames(Arrays.asList("scea1", "refedAttr2"), attributes);
    }

    @Test
    public void elementWithSimpleContentAndRestrictionAttribute() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement elementWithSimpleContentAndRestrictionAttribute =  element.get(11);
        assertEquals("elementWithSimpleContentAndRestrictionAttribute", elementWithSimpleContentAndRestrictionAttribute.getElementName());

        //elementWithSimpleContentAndRestrictionAttribute is based on elementWithSimpleContentAndExtensionAttribute but removes 'scea1'
        List<XsdAttribute> attributes = elementWithSimpleContentAndRestrictionAttribute.getAttributes();
        assertAttributeNames(Arrays.asList("refedAttr2"), attributes);
    }

    @Test
    public void testElementWithDocumentationDirectly() throws Exception {
        List<XsdElement> elements = testXsdA.getChildTargetElements();
        XsdElement element = elements.get(0);
        assertEquals("person", element.getElementName());
        assertEquals("EN", element.getDocumentation().get(0).getLanguage());
        assertEquals("Documentation blah blah en", element.getDocumentation().get(0).getContent());
        assertEquals("DE", element.getDocumentation().get(1).getLanguage());
        assertEquals("Documentation blah blah de", element.getDocumentation().get(1).getContent());
    }

    @Test
    public void testElementWithDocumentationViaRef() throws Exception {
        List<XsdElement> elements = testXsdA.getChildTargetElements();
        XsdElement happyMealRefExample = elements.get(7);
        assertEquals("happyMealRefExample", happyMealRefExample.getElementName());
        XsdElement burger = happyMealRefExample.getChildTargetElements().get(0);
        assertEquals("burger", burger.getElementName());
        assertEquals("EN", burger.getDocumentation().get(0).getLanguage());
        assertEquals("A nice jucy burger", burger.getDocumentation().get(0).getContent());
    }

    @Test
    public void testElementWithDocumentationViaComplexTypeDefinition() throws Exception {
        List<XsdElement> elements = testXsdA.getChildTargetElements();
        XsdElement elementWithChoice = elements.get(4);
        assertEquals("elementWithChoice", elementWithChoice.getElementName());
        assertEquals("ES", elementWithChoice.getDocumentation().get(0).getLanguage());
        assertEquals("Documentation for element with choice", elementWithChoice.getDocumentation().get(0).getContent());
    }

    @Test
    public void testElementWithDocumentationViaSimpleType() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement simpleTypeWithRestrictionRef =  element.get(13);
        assertEquals("simpleTypeWithRestrictionRef", simpleTypeWithRestrictionRef.getElementName());
        assertEquals("ES", simpleTypeWithRestrictionRef.getDocumentation().get(0).getLanguage());
        assertEquals("Documentation for simpleTypeWithRestrictionRef", simpleTypeWithRestrictionRef.getDocumentation().get(0).getContent());
        assertEquals("Definition", simpleTypeWithRestrictionRef.getDocumentationWithSource("Definition").getSource());
    }


    @Test
    public void testAttributeDocumentation() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement elementWithSimpleContentAndRestrictionAttribute =  element.get(11);
        assertEquals("elementWithSimpleContentAndRestrictionAttribute", elementWithSimpleContentAndRestrictionAttribute.getElementName());

        //elementWithSimpleContentAndRestrictionAttribute is based on elementWithSimpleContentAndExtensionAttribute but removes 'scea1'
        List<XsdAttribute> attributes = elementWithSimpleContentAndRestrictionAttribute.getAttributes();
        assertFalse(attributes.get(0).getDocumentation().isEmpty());
        assertEquals("EN", attributes.get(0).getDocumentation().get(0).getLanguage());
        assertEquals("Name", attributes.get(0).getDocumentation().get(0).getSource());
        assertEquals("Name en", attributes.get(0).getDocumentation().get(0).getContent());
        assertEquals("DE", attributes.get(0).getDocumentation().get(1).getLanguage());
        assertEquals("Definition", attributes.get(0).getDocumentation().get(1).getSource());
        assertEquals("Documentation blah blah de", attributes.get(0).getDocumentation().get(1).getContent());
    }

    @Test
    public void testElementWithSimpleTypeWithRestrictionEmbedded() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement simpleTypeWithRestrictionEmbedded =  element.get(12);
        assertEquals("simpleTypeWithRestrictionEmbedded", simpleTypeWithRestrictionEmbedded.getElementName());
        //this is a simple type with
        assertTrue(simpleTypeWithRestrictionEmbedded.getChildTargetElements().isEmpty());
        assertTrue(simpleTypeWithRestrictionEmbedded.getAttributes().isEmpty());
    }

    @Test
    public void testElementWithSimpleTypeAndRestrictionRef() throws Exception {
        List<XsdElement> element = testXsdA.getChildTargetElements();
        XsdElement simpleTypeWithRestrictionRef =  element.get(13);
        assertEquals("simpleTypeWithRestrictionRef", simpleTypeWithRestrictionRef.getElementName());
        //this is a simple type with
        assertTrue(simpleTypeWithRestrictionRef.getChildTargetElements().isEmpty());
        assertTrue(simpleTypeWithRestrictionRef.getAttributes().isEmpty());
    }
}
