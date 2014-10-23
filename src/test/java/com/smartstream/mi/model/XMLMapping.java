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


import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.core.proxy.AbstractCustomEntityProxy;
import scott.sort.api.core.proxy.RefNodeProxyHelper;

public class XMLMapping extends AbstractCustomEntityProxy {

    private static final long serialVersionUID = 1L;

    private final ValueNode id;
    private final RefNodeProxyHelper subSyntaxModel;
    private final ValueNode target;
    private final ValueNode xpath;
    private final RefNodeProxyHelper syntaxModel;

    public XMLMapping(Entity entity) {
        super(entity);
        id = entity.getChild("id", ValueNode.class, true);
        subSyntaxModel = new RefNodeProxyHelper(entity.getChild("subSyntaxModel", RefNode.class, true));
        target = entity.getChild("target", ValueNode.class, true);
        xpath = entity.getChild("xpath", ValueNode.class, true);
        syntaxModel = new RefNodeProxyHelper(entity.getChild("syntaxModel", RefNode.class, true));
    }

    public Long getId() {
        return id.getValue();
    }

    public void setSyntaxModel(XMLSyntaxModel syntaxModel) {
        setToRefNode(this.syntaxModel.refNode, syntaxModel);
    }

    public XMLSyntaxModel getSyntaxModel() {
        return getFromRefNode(syntaxModel.refNode);
    }

    public void setSubSyntaxModel(XMLSyntaxModel syntaxModel) {
        setToRefNode(this.subSyntaxModel.refNode, syntaxModel);
    }

    public XMLSyntaxModel getSubSyntaxModel() {
        return getFromRefNode(subSyntaxModel.refNode);
    }

    public void setXpath(String xpath) {
        this.xpath.setValue(xpath);
    }

    public String getXpath() {
        return xpath.getValue();
    }

    public void setTarget(String target) {
        this.target.setValue(target);
    }

    public String getTarget() {
        return target.getValue();
    }

}
