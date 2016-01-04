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

public class Relation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String interfaceName;

    private final RelationType relationType;

    /**
     * For a ToMany relation, the name of the node that 
     * is the FK relation back to us.
     */
    private final String foreignNodeName;
    
    /**
     * The node to sort the to many list.
     * If null, the primary key is used.
     */
    private final String sortNodeName;

    private final String joinProperty;

    public Relation(String interfaceName, RelationType relationType, String foreignNodeName, String sortNodeName, String joinProperty) {
        this.interfaceName = interfaceName;
        this.relationType = relationType;
        this.foreignNodeName = foreignNodeName;
        this.sortNodeName = sortNodeName;
        this.joinProperty = joinProperty;
    }

    Relation copy(RelationType relationType) {
        return new Relation(interfaceName, relationType, foreignNodeName, sortNodeName, joinProperty);
    }

    public String getForeignNodeName() {
        return foreignNodeName;
    }
   
    public String getSortNodeName() {
        return sortNodeName;
    }

    public String getJoinProperty() {
        return joinProperty;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    @Override
    public String toString() {
        return "StaticRelation [interfaceName=" + interfaceName + ", relationType="
                + relationType + ", foreignNodeName=" + foreignNodeName
                + ", joinProperty=" + joinProperty + "]";
    }

}
