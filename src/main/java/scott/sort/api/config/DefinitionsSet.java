package scott.sort.api.config;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all definitions from various modules
 * @author scott
 *
 */
public class DefinitionsSet {

    private Map<String, Definitions> definitionsByNamespace = new HashMap<>();

    public void addDefinitions(Definitions definitions) {
        definitionsByNamespace.put(definitions.getNamespace(), definitions);
        definitions.setDefinitionsSet(this);
    }

    /**
     *
     * @param namespace
     * @return
     * @throws IllegalStateException if not found
     */
    public Definitions getDefinitions(String namespace) {
        Definitions d = definitionsByNamespace.get(namespace);
        if (d == null) {
            throw new IllegalStateException("Definitions with namespace '" + namespace + "' not found");
        }
        return d;
    }

}
