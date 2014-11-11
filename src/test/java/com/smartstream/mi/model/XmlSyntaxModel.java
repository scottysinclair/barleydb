package com.smartstream.mi.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.proxy.ToManyNodeProxyHelper;

public class XmlSyntaxModel extends SyntaxModel {
    private static final long serialVersionUID = 1L;
    private final RefNode structure;
    private final ToManyNodeProxyHelper mappings;

    public XmlSyntaxModel(Entity entity) {
        super(entity);
        structure = entity.getChild("structure", RefNode.class, true);
        mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
    }

    public List<XmlMapping> getMappings() {
        return super.getListProxy(mappings.toManyNode);
    }

    @Override
    public XmlStructure getStructure() {
        return super.getFromRefNode(structure);
    }

    public void setStructure(XmlStructure xmlStructure) {
        setToRefNode(this.structure, xmlStructure);
    }
}
