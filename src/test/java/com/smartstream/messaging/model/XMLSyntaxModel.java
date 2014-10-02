package com.smartstream.messaging.model;

import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.proxy.ToManyNodeProxyHelper;

public class XMLSyntaxModel extends SyntaxModel {
    private static final long serialVersionUID = 1L;
    private final RefNode structure;
    private final ToManyNodeProxyHelper mappings;

    public XMLSyntaxModel(Entity entity) {
        super(entity);
        structure = entity.getChild("structure", RefNode.class, true);
        mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
    }

    public List<XMLMapping> getMappings() {
        return super.getListProxy(mappings.toManyNode);
    }

    @Override
    public XMLStructure getStructure() {
        return super.getFromRefNode(structure);
    }

    public void setStructure(XMLStructure xmlStructure) {
        setToRefNode(this.structure, xmlStructure);
    }
}
