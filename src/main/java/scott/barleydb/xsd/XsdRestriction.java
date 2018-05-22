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

import scott.barleydb.xsd.exception.InvalidXsdException;
import scott.barleydb.xsd.exception.XsdDefinitionException;

import java.util.Collections;
import java.util.List;

/**
 * <restriction
 * id=ID
 * base=QName
 * any attributes
 * >
 *
 *Content for simpleType:
 *(annotation?,(simpleType?,(minExclusive|minInclusive|
 *maxExclusive|maxInclusive|totalDigits|fractionDigits|
 *length|minLength|maxLength|enumeration|whiteSpace|pattern)*))
 *
 *Content for simpleContent:
 *(annotation?,(simpleType?,(minExclusive |minInclusive|
 *maxExclusive|maxInclusive|totalDigits|fractionDigits|
 *length|minLength|maxLength|enumeration|whiteSpace|pattern)*)?,
 *((attribute|attributeGroup)*,anyAttribute?))
 *
 *Content for complexContent:
 *(annotation?,(group|all|choice|sequence)?,
 *((attribute|attributeGroup)*,anyAttribute?))
 * </restriction>
 */
public final class XsdRestriction implements XsdNode {
    private final XsdNode parent;
    private final Element domElement;

    public XsdRestriction(XsdNode parent, Element domElement) {
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
        return Common.getDocumentation(domElement);
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        if (parent instanceof XsdComplexContent || parent instanceof XsdSimpleContent) {
            //only attribute and attribute group are relevant for attributes
            return Common.getAttributes(this, domElement, ATTRIBUTE, ATTRIBUTEGROUP);
        }
        return Collections.emptyList();
    }


    /**
     * A restiction defines the child target elements explicitly
     * @return
     * @throws InvalidXsdException
     */
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        if (parent instanceof XsdComplexContent) {
            return Common.getChildElementsFromOneOf(this, domElement, GROUP, ALL, CHOICE, SEQUENCE);
        }
        return Collections.emptyList();
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

}
