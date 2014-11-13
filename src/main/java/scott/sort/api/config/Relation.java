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

    private final String foreignNodeName;

    private final String joinProperty;

    public Relation(String interfaceName, RelationType relationType, String foreignNodeName, String joinProperty) {
        this.interfaceName = interfaceName;
        this.relationType = relationType;
        this.foreignNodeName = foreignNodeName;
        this.joinProperty = joinProperty;
    }

    Relation copy(RelationType relationType) {
        return new Relation(interfaceName, relationType, foreignNodeName, joinProperty);
    }

    public String getForeignNodeName() {
        return foreignNodeName;
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
