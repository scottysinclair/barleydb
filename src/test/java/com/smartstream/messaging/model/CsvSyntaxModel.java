package com.smartstream.messaging.model;

import java.util.List;

import com.smartstream.sort.api.core.entity.Entity;
import com.smartstream.sort.api.core.entity.RefNode;
import com.smartstream.sort.api.core.entity.ToManyNode;
import com.smartstream.sort.api.core.proxy.ToManyNodeProxyHelper;

public class CsvSyntaxModel extends SyntaxModel {

    private static final long serialVersionUID = 1L;
    private final RefNode structure;
    private final ToManyNodeProxyHelper mappings;

    public CsvSyntaxModel(Entity entity) {
        super(entity);
        structure = entity.getChild("structure", RefNode.class, true);
        mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
    }

    public List<CsvMapping> getMappings() {
        return super.getListProxy(mappings.toManyNode);
    }

    @Override
    public CsvStructure getStructure() {
        return super.getFromRefNode(structure);
    }

    public void setStructure(CsvStructure csvStructure) {
        setToRefNode(this.structure, csvStructure);
    }
}