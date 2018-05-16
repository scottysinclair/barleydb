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
 * The simpleContent element contains extensions or restrictions on a text-only complex type or on a simple type as content and contains no elements.
 *
 * Parent elements: complexType
 *
 * <simpleContent
 * id=ID
 * any attributes
 * >
 *
 * (annotation?,(restriction|extension))
 *
 * </simpleContent>
 */
public class XsdSimpleContent implements XsdNode {
    private final XsdNode parent;
    private final Element domElement;

    public XsdSimpleContent(XsdNode parent, Element domElement) {
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

    /**
     * Simple content has no elements
     * @return
     * @throws XsdDefinitionException
     */
    @Override
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException {
        return Collections.emptyList();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        return Common.getAttributes(this, domElement, RESTRICTION, EXTENSION);
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }
}
