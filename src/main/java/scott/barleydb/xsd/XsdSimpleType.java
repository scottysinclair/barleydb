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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.XsdDefinitionException;

import java.util.Collections;
import java.util.List;

import static scott.barleydb.xsd.SchemaElements.RESTRICTION;

/**
 *
 * The simpleType element defines a simple type and specifies the constraints and information about the values of attributes or text-only elements.
 *
 *
 * Parent elements: attribute, element, list, restriction, schema, union
 *
 *<simpleType
 *id=ID
 *name=NCName
 *any attributes
 *>
 *
 * (annotation?,(restriction|list|union))
 *
 * </simpleType>
 *
 * The restriction element cannot define any attributes, so we have no attributes or child target elements
 *
 */
public class XsdSimpleType implements XsdNode, XsdType {

    private final XsdNode parent;
    private final Element domElement;

    public XsdSimpleType(XsdNode parent, Element domElement) {
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
    public boolean isAbstract() {
        return false;
    }

    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        List<XsdDocumentation> documentation = Common.getDocumentation(domElement);
        if (!documentation.isEmpty()) {
            return documentation;
        }
        //if we have a restriction, then use it to get the documentation of this type
        XsdNode xsdRestriction= Common.getXsdNode(this, domElement, RESTRICTION);
        if (xsdRestriction != null) {
            return xsdRestriction.getDocumentation();
        }
        //there is no documentation just return the empty list
        return documentation;

    }

    @Override
    public XsdElement getElement(QualifiedName qualifiedName) {
        return null;
    }

    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

    @Override
    public String getTypeLocalName() {
        if (domElement.hasAttribute("name")) {
            String rawName = domElement.getAttribute("name");
            int p;
            if ((p=rawName.lastIndexOf(':'))!=-1) {
                return rawName.substring(p+1);
            } else {
                return rawName;
            }
        }
        return "";
    }

    @Override
    public String getTypeNamespaceUri() {
        XsdDefinition xsd = getXsdDefinition();
        return xsd.getTargetNamespace();
    }

    @Override
    public String toString() {
        return String.valueOf(getTypeNamespaceUri()) + " " + getTypeLocalName();
    }

}
