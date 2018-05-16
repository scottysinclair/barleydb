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

import scott.barleydb.xsd.exception.InvalidXsdException;

/**
 * Supported schema elements
 *
 *
 */
public enum SchemaElements {
    ELEMENT("element"),
    ATTRIBUTE("attribute"),
    ATTRIBUTEGROUP("attributeGroup"),
    ALL("all"),
    ANY("any"),
    CHOICE("choice"),
    SIMPLETYPE("simpleType"),
    SIMPLECONTENT("simpleContent"),
    COMPLEXTYPE("complexType"),
    COMPLEXCONTENT("complexContent"),
    EXTENSION("extension"),
    RESTRICTION("restriction"),
    GROUP("group"),
    SEQUENCE("sequence");

    /**
     *
     * @param localName
     * @return the SchemaElement matching the localName
     * @throws InvalidXsdException if the local name is not in the enum
     */
    public static SchemaElements toSchemaElement(String localName) throws InvalidXsdException {
        for (SchemaElements se: values()) {
            if (se.getLocalName().equals(localName)) {
                return se;
            }
        }
        throw new InvalidXsdException("'" + localName + "' is not supported in XSD definition");
    }

    private final String localName;

    private SchemaElements(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return localName;
    }
}
