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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Relation {

    @XmlAttribute
    private final String interfaceName;

    @XmlAttribute
    private final RelationType relationType;

    @XmlAttribute
    private final String foreignNodeName;

    @XmlAttribute
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
        return "Relation [interfaceName=" + interfaceName + ", relationType="
                + relationType + ", foreignNodeName=" + foreignNodeName
                + ", joinProperty=" + joinProperty + "]";
    }

}
