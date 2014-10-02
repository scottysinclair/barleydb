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

import java.util.LinkedList;
import java.util.List;

public class ConfigHelper {

    public static List<String> getInterfaces(EntityType... a) {
        List<String> result = new LinkedList<String>();
        for (EntityType et : a) {
            result.add(et.getInterfaceName());
        }
        return result;
    }

}
