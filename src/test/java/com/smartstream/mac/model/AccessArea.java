package com.smartstream.mac.model;

import java.util.List;

public interface AccessArea {
    Long getId();

    void setName(String name);

    String getName();

    AccessArea getParent();

    void setParent(AccessArea parent);

    List<AccessArea> getChildren();
}
