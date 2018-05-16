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

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.XsdDefinitionException;

/**
 * Represents a type from the http://www.w3.org/2001/XMLSchema namespace
 */
public class XsdCoreType implements XsdNode, XsdType {
    private final String name;
    private final XsdNode parent;

    XsdCoreType(XsdNode parent, String name) {
        String rawName = name;
        int p;
        if ((p=rawName.lastIndexOf(':'))!=-1) {
            this.name = rawName.substring(p+1);
        } else {
            this.name = name;
        }
        this.parent = parent;
    }

    @Override
    public Node getDomNode() {
        return null;
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
    public boolean isAbstract() {
        return false;
    }

    @Override
    public XsdElement getElement(QualifiedName qualifiedName) {
        return null;
    }

    @Override
    public XsdDefinition getXsdDefinition() {
        throw new UnsupportedOperationException("XSD core types do not have an XsdDefinition model");
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
        return name;
    }

    @Override
    public String getTypeNamespaceUri() {
        return "http://www.w3.org/2001/XMLSchema";
    }

    @Override
    public String toString() {
        return String.valueOf(getTypeNamespaceUri()) + " " + getTypeLocalName();
    }

}
