package scott.barleydb.api.specification;

import java.io.Serializable;
import java.util.Collections;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

@XmlAccessorType(XmlAccessType.NONE)
public class EnumSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlID
    @XmlAttribute
    private String className;

    @XmlElement
    private List<EnumValueSpec> enumValues = null;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<EnumValueSpec> getEnumValues() {
        return Collections.unmodifiableList( enumValues );
    }

    public void setEnumValues(List<EnumValueSpec> enumValues) {
        this.enumValues = enumValues;
    }

    @Override
    public String toString() {
        return "EnumSpec [className=" + className + ", enumValues=" + enumValues + "]";
    }


}
