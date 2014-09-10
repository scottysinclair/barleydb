package com.smartstream.morf.api.config;

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
