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

import scott.barleydb.xsd.exception.PrefixNotResolvedException;

/**
 * Represents a qualified name an optional namespace and local name
 *
 * If the namespace could not be resolved from the prefix then the prefix will
 * be stored in the unresolvedPrefix property.
 *
 */
public final class QualifiedName {

    private final String namespace;

    private final String localName;

    private final String unresolvedPrefix;

    public QualifiedName(XsdDefinition xsdDefinition, String name, boolean failIfPrefixNotResolved) throws PrefixNotResolvedException {
        this(xsdDefinition, name);
        if (failIfPrefixNotResolved  && unresolvedPrefix != null) {
            throw new PrefixNotResolvedException("Could not resolve namespace prefix of complexType '" + name + "'");
        }
    }

    public QualifiedName(XsdDefinition xsdDefinition, String name) {
        int i = name.indexOf(':');
        if (i == -1) {
            /*
             * there is no prefix, the namespace is either the defined default namespace or the NO NAMESPACE
             */
            namespace = xsdDefinition.getDefaultNamespace();
            localName = name;
            unresolvedPrefix = null;
        }
        else {
            final String prefix = name.substring(0, i);
            namespace = xsdDefinition.resolvePrefix( prefix );
            unresolvedPrefix = namespace == null ? prefix : null;
            localName = name.substring(i+1);
        }
    }

    public QualifiedName(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
        this.unresolvedPrefix = null;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getLocalName() {
        return localName;
    }

    public boolean hasUnresolvedPrefix() {
        return unresolvedPrefix != null;
    }

    /**
     * the prefix which could not be resolved into a namespace
     * @return the prefix or null if there were no problems.
     */
    public String getUnresolvedPrefix() {
        return unresolvedPrefix;
    }

    @Override
    public String toString() {
        return "QualifiedName [namespace=" + namespace + ", localName=" + localName + "]";
    }


}
