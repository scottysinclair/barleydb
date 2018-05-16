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
 * <attributeGroup
 * id=ID
 * name=NCName
 * ref=QName
 * any attributes
 * >
 *
 * (annotation?),((attribute|attributeGroup)*,anyAttribute?))
 *
 *</attributeGroup>
 */
public class XsdAttributeGroup implements XsdNode {
    private final XsdNode parent;
    private final Element domElement;

    public XsdAttributeGroup(XsdNode parent, Element domElement) {
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
        return Collections.emptyList();
    }

    @Override
    public List<XsdAttribute> getAttributes() throws XsdDefinitionException {
        XsdAttributeGroup ref = resolveReference();
        if (ref != null) {
            return ref.getAttributes();
        }
        return Common.getAttributes(this, domElement, ATTRIBUTE, ATTRIBUTEGROUP);
    }

    /**
     * If we are an attribute group reference, then return the resolved attribute group
     * @return
     */
    private XsdAttributeGroup resolveReference() throws XsdDefinitionException {
        String ref = domElement.getAttribute("ref");
        if (ref.isEmpty()) {
            return null;
        }
        return getXsdDefinition().findAttributeGroup(new QualifiedName(getXsdDefinition(), ref));
    }

    @Override
    public XsdNode getParent() {
        return parent;
    }
}
