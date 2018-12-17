package scott.barleydb.api.config;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Contains all definitions from various modules
 *
 * @author scott
 *
 */
public class DefinitionsSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Definitions> definitionsByNamespace = new HashMap<>();

    public void addDefinitions(Definitions definitions) {
        if (!definitionsByNamespace.containsKey(definitions.getNamespace())) {
            definitionsByNamespace.put(definitions.getNamespace(), definitions);
            definitions.setDefinitionsSet(this);
        }
    }
    
    public EntityType getFirstEntityTypeByInterfaceName(String interfaceName) {
    	for (Definitions def: definitionsByNamespace.values()) {
    		EntityType et = def.getEntityTypeMatchingInterface(interfaceName, false);
    		if (et != null) {
    			return et;
    		}
    	}
    	return null;
    }

    public void addAll(DefinitionsSet definitionsSet) {
        definitionsByNamespace.putAll(definitionsSet.definitionsByNamespace);
    }

    /**
     *
     * @param namespace
     * @return
     * @throws IllegalStateException
     *             if not found
     */
    public Definitions getDefinitions(String namespace) {
        Definitions d = definitionsByNamespace.get(namespace);
        if (d == null) {
            throw new IllegalStateException("Definitions with namespace '" + namespace + "' not found");
        }
        return d;
    }

    public Iterable<Definitions> getDefinitions() {
        return Collections.unmodifiableCollection(definitionsByNamespace.values());
    }

}
