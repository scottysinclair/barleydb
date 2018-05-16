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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * The extension element extends an existing simpleType or complexType element
 *
 * <extension
 * id=ID
 * base=QName
 * any attributes
 * >

 * (annotation?,((group|all|choice|sequence)?,
 * ((attribute|attributeGroup)*,anyAttribute?)))
 *
 * </extension>
 */
public class XsdExtension implements XsdNode {
    private final XsdNode parent;
    private final Element domElement;

    public XsdExtension(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
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
        String base = domElement.getAttribute("base");
        if (base.isEmpty()) {
            throw new XsdDefinitionException("base must be specified on the extension element");
        }
        XsdNode xsdType = getXsdDefinition().findType( new QualifiedName(getXsdDefinition(), base, true) );

        /*
         * add the attributes from the base type
         */
        List<XsdAttribute> attributes = new LinkedList<>();
        attributes.addAll( xsdType.getAttributes() );

        attributes.addAll( Common.getAttributes(this, domElement, ATTRIBUTE, ATTRIBUTEGROUP) );
        return attributes;
    }

    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        String base = domElement.getAttribute("base");
        if (base.isEmpty()) {
            throw new XsdDefinitionException("base must be specified on the extension element");
        }
        XsdNode xsdType = getXsdDefinition().findType( new QualifiedName(getXsdDefinition(), base, true) );
        /*
         * add the elements from the base type
         */
        List<XsdElement> elements = new LinkedList<>();
        elements.addAll( xsdType.getChildTargetElements() );

        /*
         * then add any other elements from our own definition
         */
        //use (group|all|choice|sequence)? to get child elements
        elements.addAll( Common.getChildElementsFromOneOf(this, domElement, GROUP, ALL, CHOICE, SEQUENCE) );
        return elements;
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

    /**
     * @return the XsdSequence child if it contains one, null otherwise.
     * @throws XsdDefinitionException
     */
    public XsdSequence getSequence() throws XsdDefinitionException {
        return (XsdSequence) Common.getXsdNode(this, domElement, SEQUENCE);
    }
}
