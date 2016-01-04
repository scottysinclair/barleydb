package com.smartstream.mac;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import static scott.sort.api.specification.CoreSpec.mandatoryRefersTo;
import static scott.sort.api.specification.CoreSpec.optionallyRefersTo;
import static scott.sort.api.specification.CoreSpec.uniqueConstraint;
import static scott.sort.api.specification.CoreSpec.refersToMany;

import com.smartstream.MorpheusSpec;

import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;
import scott.sort.build.specification.staticspec.Entity;

public class MacSpec extends MorpheusSpec {

    public MacSpec() {
        super("com.smartstream.mac");
    }

    @Entity("MAC_ACCESS_AREA")
    public static class AccessArea {
        public static NodeSpec id = longPrimaryKey();

        public static NodeSpec name = name();

        public static NodeSpec modifiedAt = optimisticLock();

        //public static NodeSpec uuid = uuid();

        public static NodeSpec parent = optionallyRefersTo(AccessArea.class, "PARENT_ID");

        public static NodeSpec children =  refersToMany(AccessArea.class, AccessArea.parent);

    }

    @Entity("MAC_USER")
    public static class User implements TopLevelModel {

        public static NodeSpec name = name("USER_NAME");
    }

    public interface TopLevelModel {
        NodeSpec id = longPrimaryKey();

        NodeSpec accessArea = mandatoryRefersTo(AccessArea.class);

        NodeSpec uuid = uuid();

        NodeSpec modifiedAt = optimisticLock();

        NodeSpec name = name();

        UniqueConstraintSpec nameAndAccessArea = uniqueConstraint(name, accessArea);
    }

}