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

import java.util.List;

import org.w3c.dom.Node;

import scott.barleydb.xsd.exception.XsdDefinitionException;

public interface XsdType extends XsdNode {

    public Node getDomNode();
    /**
     * documentation for the given type
     * @return the list of documentation or the empty list
     */
    public List<XsdDocumentation> getDocumentation() throws XsdDefinitionException;
    public List<XsdElement> getChildTargetElements() throws XsdDefinitionException;
    /**
     * @return the local name of the type or empty string if it is anonymous.
     */
    public String getTypeLocalName();
    public String getTypeNamespaceUri();
    public XsdElement getElement(QualifiedName qualifiedName) throws XsdDefinitionException;

    /**
     *@return true if the type is abstract.
     */
    public boolean isAbstract();
}
