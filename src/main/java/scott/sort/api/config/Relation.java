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
