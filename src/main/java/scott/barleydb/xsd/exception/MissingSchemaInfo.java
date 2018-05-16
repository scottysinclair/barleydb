package scott.barleydb.xsd.exception;

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

import java.io.Serializable;

/**
 *
 * Information about a missing schema
 *
 */
public class MissingSchemaInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum DependencyType {
        IMPORT,
        INCLUDE
    }

    private final DependencyType type;
    private final String namespaceUri;
    private final String schemaLocation;

    public MissingSchemaInfo(DependencyType type, String namespaceUri, String schemaLocation) {
        this.type = type;
        this.namespaceUri = namespaceUri;
        this.schemaLocation = schemaLocation;
    }

    public DependencyType getType() {
        return type;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void appendExceptionMessage(StringBuilder sb) {
        sb.append("Could not resolve schema ")
           .append(type)
           .append(" '")
           .append(schemaLocation)
           .append("' with namesaceUri='")
           .append(namespaceUri).append("'");
    }

    @Override
    public String toString() {
        return "MissingSchemaInfo{" +
                "type=" + type +
                ", namespaceUri='" + namespaceUri + '\'' +
                ", schemaLocation='" + schemaLocation + '\'' +
                '}';
    }
}
