package scott.sort.api.specification;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "SpecRegistry")
@XmlAccessorType(XmlAccessType.NONE)
public class SpecRegistry {

    @XmlJavaTypeAdapter(DefinitionsSpecAdapter.class)
    @XmlElement(name="contents")
    private final LinkedHashMap<String,DefinitionsSpec> definitionsByNamespace = new LinkedHashMap<>();

    public void add(DefinitionsSpec definitionsSpec) {
        this.definitionsByNamespace.put(definitionsSpec.getNamespace(), definitionsSpec);
    }

    public DefinitionsSpec getDefinitionsSpec(String namespace) {
        return definitionsByNamespace.get( namespace );
    }

    public static class DefinitionsList {
        @XmlElement(name="Definitions")
        private final List<DefinitionsSpec> data = new LinkedList<>();
    }

    public static class DefinitionsSpecAdapter extends XmlAdapter<DefinitionsList, LinkedHashMap<String,DefinitionsSpec>> {
        @Override
        public LinkedHashMap<String, DefinitionsSpec> unmarshal(DefinitionsList nodeSpecs) throws Exception {
            LinkedHashMap<String, DefinitionsSpec> map = new LinkedHashMap<>();
            for (DefinitionsSpec spec: nodeSpecs.data) {
                map.put(spec.getNamespace(), spec);
            }
            return map;
        }

        @Override
        public DefinitionsList marshal(LinkedHashMap<String, DefinitionsSpec> entitySpecs) throws Exception {
            DefinitionsList list = new DefinitionsList();
            list.data.addAll( entitySpecs.values() );
            return list;
        }
    }

}
