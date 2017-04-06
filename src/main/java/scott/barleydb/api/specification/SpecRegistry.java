package scott.barleydb.api.specification;

import java.util.Collections;

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
    @XmlElement(name = "contents")
    private final LinkedHashMap<String, DefinitionsSpec> definitionsByNamespace = new LinkedHashMap<>();

    public void add(DefinitionsSpec definitionsSpec) {
        this.definitionsByNamespace.put(definitionsSpec.getNamespace(), definitionsSpec);
    }

    public DefinitionsSpec getDefinitionsSpec(String namespace) {
        return definitionsByNamespace.get(namespace);
    }

    public static class DefinitionsList {
        @XmlElement(name = "Definitions")
        private final List<DefinitionsSpec> data = new LinkedList<>();
    }

    public static class DefinitionsSpecAdapter
            extends XmlAdapter<DefinitionsList, LinkedHashMap<String, DefinitionsSpec>> {
        @Override
        public LinkedHashMap<String, DefinitionsSpec> unmarshal(DefinitionsList nodeSpecs) throws Exception {
            LinkedHashMap<String, DefinitionsSpec> map = new LinkedHashMap<>();
            for (DefinitionsSpec spec : nodeSpecs.data) {
                map.put(spec.getNamespace(), spec);
            }
            return map;
        }

        @Override
        public DefinitionsList marshal(LinkedHashMap<String, DefinitionsSpec> entitySpecs) throws Exception {
            DefinitionsList list = new DefinitionsList();
            list.data.addAll(entitySpecs.values());
            return list;
        }
    }

    public Iterable<DefinitionsSpec> getDefinitions() {
        return Collections.unmodifiableCollection(definitionsByNamespace.values());
    }

}
