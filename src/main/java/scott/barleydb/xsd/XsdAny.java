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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * The XsdAny object allows the content to be programatically set.
 *
 * The any element enables the author to extend the XML document with elements not specified by the schema
 *
 * Parent elements: choice, sequence
 *
 * <any
 * id=ID
 * maxOccurs=nonNegativeInteger|unbounded
 * minOccurs=nonNegativeInteger
 * namespace=namespace
 * processContents=lax|skip|strict
 * any attributes
 * >
 *
 * (annotation?)
 *
 * </any>
 */
public class XsdAny implements XsdNode {

    private final XsdNode parent;
    private final Element domElement;

    public XsdAny(XsdNode parent, Element domElement) {
        this.parent = parent;
        this.domElement = domElement;
    }

    @Override
    public Node getDomNode() {
      return domElement;
    }

    public XsdNode getContent() {
        return getXsdDefinition().getXsdAnyContent(this);
    }

    public void setContent(XsdNode content) {
        getXsdDefinition().setXsdAnyContent(this, content);
    }

    @Override
    public XsdDefinition getXsdDefinition() {
        return parent.getXsdDefinition();
    }

    /**
     * No documentation
     * @return
     * @throws XsdDefinitionException
     */
    @Override
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException {
        XsdNode content = getContent();
        if (content != null) {
            return content.getDocumentation();
        }
        return Collections.emptyList();
    }

    /**
     * we can't know if there are any child elements.
     * @return
     * @throws XsdDefinitionException
     */
    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        XsdNode content = getContent();
        if (content != null) {
            if (content instanceof XsdElement) {
                return new ArrayList<XsdElement>(Arrays.asList((XsdElement)content));
            }
            return content.getChildTargetElements();
        }
        return Collections.emptyList();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        XsdNode content = getContent();
        if (content != null) {
            return content.getAttributes();
        }
        return Collections.emptyList();
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }

    public Node getDomElement() {
        return domElement;
    }

}
