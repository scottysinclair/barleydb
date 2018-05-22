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

import static scott.barleydb.xsd.SchemaElements.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.XsdDefinitionException;

import java.util.*;

/**
 *
 * XML Schema choice element allows only one of the elements contained in the <choice> declaration to be present within the containing element.
 *
 *
 * Parent elements: group, choice, sequence, complexType, restriction (both simpleContent and complexContent), extension (both simpleContent and complexContent)
 *
 * <choice
 * id=ID
 * maxOccurs=nonNegativeInteger|unbounded
 * minOccurs=nonNegativeInteger
 * any attributes
 * >
 *
 * (annotation?,(element|group|choice|sequence|any)*)
 *
 * </choice>
 */
public class XsdChoice implements XsdNode {

    private final XsdNode parent;
    private final Element domElement;

    public XsdChoice(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
    }

    @Override
    public Node getDomNode() {
      return domElement;
    }

    @Override
    public XsdDefinition getXsdDefinition() {
        return parent.getXsdDefinition();
    }

    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        //(element|group|choice|sequence|any)*

        List<XsdElement> allElementsAcrossAllChoices = Common.getChildElementsAcrossNodes(this, domElement, ELEMENT, GROUP, CHOICE, SEQUENCE, ANY);
       /*
        * Each choice branch can generate the same XsdElement, we have to make sure that we have no dups
        */
        filterOutDups(allElementsAcrossAllChoices);

        return  allElementsAcrossAllChoices;
    }

    private void filterOutDups(Collection<XsdElement> elements) throws XsdDefinitionException {
        Set<String> elementKeys = new HashSet<>();
        for (Iterator<XsdElement> i = elements.iterator(); i.hasNext();) {
            XsdElement xsdElement = i.next();
            final String key = String.valueOf(xsdElement.getNamespaceUri()) + "." + xsdElement.getElementName();
            if (!elementKeys.add(key)) {
                i.remove();
            }
        }
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }
}
