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

import java.util.List;

/**
 *
 * XsdComplexType definition
 *
 * <complexType
 * id=ID
 *  name=NCName
 * abstract=true|false
 * mixed=true|false
 * block=(#all|list of (extension|restriction))
 * final=(#all|list of (extension|restriction))
 * any attributes
 * >
 *
 * (annotation?,(simpleContent|complexContent|((group|all|choice|sequence)?,((attribute|attributeGroup)*,anyAttribute?))))
 * optional annotation

 *
 * </complexType>
 *
 *
 *
 */
public class XsdComplexType implements XsdNode, XsdType {

    private XsdNode parent;
    private final Element domElement;

    public XsdComplexType(XsdNode parent, Element domElement) {
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
        return "true".equals( domElement.getAttribute("abstract") );
    }

    /**
     * @return the Complex Content child, if it exists, null otherwise
     */
    public XsdComplexContent getComplexContent() throws XsdDefinitionException {
        return (XsdComplexContent)Common.getXsdNode(this, domElement, COMPLEXCONTENT);
    }

    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        List<XsdDocumentation> documentation = Common.getDocumentation(domElement);
        if (!documentation.isEmpty()) {
            return documentation;
        }
        //if we extend or restrict another type then we can fall back to the documentation defined there.
        XsdNode xsdComplexContent = Common.getXsdNode(this, domElement, COMPLEXCONTENT);
        if (xsdComplexContent != null) {
            return xsdComplexContent.getDocumentation();
        }
        //there is no documentation just return the empty list
        return documentation;
    }

    @Override
    public XsdElement getElement(QualifiedName qualifiedName) throws XsdDefinitionException {
        for (XsdElement element: getChildTargetElements()) {
            if (element.matches(qualifiedName)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        return Common.getChildElementsFromOneOf(this, domElement, COMPLEXCONTENT, GROUP, ALL, CHOICE, SEQUENCE);
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        //only simpleContent, complextContent, attribute and attributeGroup can define attributes for a type
        return Common.getAttributes(this, domElement, SIMPLECONTENT, COMPLEXCONTENT, ATTRIBUTE, ATTRIBUTEGROUP);
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
        return getXsdDefinition().getTargetNamespace();
    }

    @Override
    public String toString() {
        return String.valueOf(getTypeNamespaceUri()) + " " + getTypeLocalName();
    }

}
