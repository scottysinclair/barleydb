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

import scott.barleydb.xsd.exception.XsdDefinitionException;

import java.util.Collections;
import java.util.List;

/**
 *
 * The group element is used to define a group of elements to be used in complex type definitions
 *
 * This can actually be a group definition or a group reference depending on whether the name or ref attribute is used
 *
 * Parent elements: schema, choice, sequence, complexType, restriction (both simpleContent and complexContent), extension (both simpleContent and complexContent)
 *
 * <group
 * id=ID
 * name=NCName
 * ref=QName
 * maxOccurs=nonNegativeInteger|unbounded
 * minOccurs=nonNegativeInteger
 * any attributes
 * >
 *
 * (annotation?,(all|choice|sequence)?)
 *
 * </group>
 */
public class XsdGroup implements XsdNode {

    private final XsdNode parent;
    private final Element domElement;

    public XsdGroup(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
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
        String groupRef = domElement.getAttribute("ref");
        if (groupRef.isEmpty()) {
            //we are a group definition
            return Common.getChildElementsFromOneOf(this, domElement, ALL, CHOICE, SEQUENCE);
        }
        else {
            //lookup the ref
            XsdGroup xsdGroup = getXsdDefinition().findGroup(new QualifiedName(getXsdDefinition(), groupRef, true));
            return xsdGroup.getChildTargetElements();
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

    public XsdElement getElement(QualifiedName qualifiedName) throws XsdDefinitionException {
        for (XsdElement element: getChildTargetElements()) {
            if (element.matches(qualifiedName)) {
                return element;
            }
        }
        return null;
    }
}
