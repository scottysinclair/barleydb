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

import static scott.barleydb.xsd.exception.MissingSchemaInfo.DependencyType;

import java.util.LinkedList;
import java.util.List;

public class SchemaNotFoundException extends XsdDefinitionException {

    private static final long serialVersionUID = 1L;

    /**
     * returns an exception which has the missing schemas from a and b
     * @param a can be null
     * @param b must not be null
     * @return the new exception or b is a is null
     */
    public static SchemaNotFoundException merge(SchemaNotFoundException a, SchemaNotFoundException b) {
        if (a == null) {
            return b;
        }
        return new SchemaNotFoundException(a, b);
    }



    private List<MissingSchemaInfo> missingSchemas;

    public SchemaNotFoundException(DependencyType dependencyType, String namespaceUri, String schemaLocation) {
        super("Could not resolve schema '" + schemaLocation + "' with namesaceUri='" + namespaceUri + "'");
        missingSchemas = new LinkedList<>();
        missingSchemas.add(new MissingSchemaInfo(dependencyType, namespaceUri, schemaLocation));
    }

    private SchemaNotFoundException(SchemaNotFoundException a, SchemaNotFoundException b) {
        super(concatMessages(a, b));
        missingSchemas = new LinkedList<>();
        missingSchemas.addAll( a.getMissingSchemas() );
        missingSchemas.addAll( b.getMissingSchemas() );
    }

    public List<MissingSchemaInfo> getMissingSchemas() {
        return missingSchemas;
    }

    private static String concatMessages(SchemaNotFoundException a, SchemaNotFoundException b) {
        StringBuilder sb = new StringBuilder();
        List<MissingSchemaInfo> all = new LinkedList<>(a.getMissingSchemas());
        all.addAll(b.getMissingSchemas());
        for (MissingSchemaInfo missingSchemaInfo: all) {
            missingSchemaInfo.appendExceptionMessage(sb);
            sb.append('\n');
        }
        sb.setLength(sb.length()-1);
        return sb.toString();
    }
}
