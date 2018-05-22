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

/*
*
* The complexContent element defines extensions or restrictions on a complex type that contains mixed content or elements only
* <complexContent
* id=ID
* mixed=true|false
* any attributes
* >
*
* (annotation?,(restriction|extension))
*
* </complexContent>
 */
public class XsdComplexContent implements XsdNode {

    private final XsdNode parent;

    private final Element domElement;

    public XsdComplexContent(XsdNode parent, Element domElement) {
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

    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        List<XsdDocumentation> documentation = Common.getDocumentation(domElement);
        if (!documentation.isEmpty()) {
            return documentation;
        }
        return getRestrictionOrExtension().getDocumentation();
    }


    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        return getRestrictionOrExtension().getChildTargetElements();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return getRestrictionOrExtension().getAttributes();
    }

    /**
     * @return the XsdExtension child declaration, if it exists.
     */
    public XsdExtension getExtension() throws XsdDefinitionException {
        return (XsdExtension) Common.getXsdNode(this, domElement, EXTENSION);
    }

    private XsdNode getRestrictionOrExtension() throws XsdDefinitionException {
        XsdNode xsdNode = Common.getXsdNode(this, domElement, RESTRICTION, EXTENSION);
        if (xsdNode == null) {
            throw new XsdDefinitionException("ComplexContext must be either a restriction or extension");
        }
        return xsdNode;
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }
}
