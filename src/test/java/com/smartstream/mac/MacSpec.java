package com.smartstream.mac;

import static scott.sort.api.specification.CoreSpec.mandatoryRefersTo;
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

        public static NodeSpec parent = mandatoryRefersTo(AccessArea.class);

        public static NodeSpec children =  refersToMany(AccessArea.class, AccessArea.parent);

        public static NodeSpec uuid = uuid();
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