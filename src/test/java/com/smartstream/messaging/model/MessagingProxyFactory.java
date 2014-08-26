package com.smartstream.messaging.model;

import java.util.List;

import com.smartstream.mac.model.User;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.api.core.entity.ValueNode;
import com.smartstream.morf.api.core.proxy.AbstractCustomEntityProxy;
import com.smartstream.morf.api.core.proxy.ProxyFactory;
import com.smartstream.morf.api.core.proxy.RefNodeProxyHelper;
import com.smartstream.morf.api.core.proxy.ToManyNodeProxyHelper;

public class MessagingProxyFactory implements ProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T newProxy(Entity entity) {
        if (entity.getEntityType().getInterfaceName().equals(XMLSyntaxModel.class.getName())) {
            return (T)new XMLSyntaxModelProxy(entity);
        }
        if (entity.getEntityType().getInterfaceName().equals(XMLMapping.class.getName())) {
            return (T)new XMLMappingProxy(entity);
        }
        return null;
    }

}

class XMLSyntaxModelProxy extends AbstractCustomEntityProxy implements XMLSyntaxModel {
    private static final long serialVersionUID = 1L;
    private final ValueNode id;
    private final ValueNode name;
    private final ValueNode syntaxType;
    private final RefNodeProxyHelper user;
    private final RefNode structure;
    private final ToManyNodeProxyHelper mappings;

    public XMLSyntaxModelProxy(Entity entity) {
        super(entity);
        id = entity.getChild("id", ValueNode.class, true);
        name = entity.getChild("name", ValueNode.class, true);
        syntaxType = entity.getChild("syntaxType", ValueNode.class, true);
        user = new RefNodeProxyHelper(entity.getChild("user", RefNode.class, true));
        structure = entity.getChild("structure", RefNode.class, true);
        mappings = new ToManyNodeProxyHelper(entity.getChild("mappings", ToManyNode.class, true));
    }

    @Override
    public Long getId() {
        return id.getValue();
    }

    @Override
    public String getName() {
        return name.getValue();
    }

    @Override
    public void setName(String name) {
        this.name.setValue(name);
    }

    @Override
    public SyntaxType getSyntaxType() {
        return syntaxType.getValue();
    }

    @Override
    public void setSyntaxType(SyntaxType syntaxType) {
        this.syntaxType.setValue(syntaxType);
    }

    @Override
    public User getUser() {
        return super.getFromRefNode(user.refNode);
    }

    @Override
    public void setUser(User user) {
        setToRefNode(this.user.refNode, user);
    }

    @Override
    public List<XMLMapping> getMappings() {
        return super.getListProxy(mappings.toManyNode);
    }

    @Override
    public XMLStructure getStructure() {
        return super.getFromRefNode(structure);
    }

    @Override
    public void setStructure(XMLStructure xmlStructure) {
        setToRefNode(this.structure, xmlStructure);
    }
}


class XMLMappingProxy extends AbstractCustomEntityProxy implements XMLMapping {

    private static final long serialVersionUID = 1L;

    private final ValueNode id;
    private final RefNodeProxyHelper subSyntaxModel;
    private final ValueNode target;
    private final ValueNode xpath;
    private final RefNodeProxyHelper syntaxModel;

    public XMLMappingProxy(Entity entity) {
        super(entity);
        id = entity.getChild("id", ValueNode.class, true);
        subSyntaxModel = new RefNodeProxyHelper(entity.getChild("subSyntaxModel", RefNode.class, true));
        target = entity.getChild("target", ValueNode.class, true);
        xpath = entity.getChild("xpath", ValueNode.class, true);
        syntaxModel = new RefNodeProxyHelper(entity.getChild("syntaxModel", RefNode.class, true));
    }

    @Override
    public Long getId() {
        return id.getValue();
    }

    @Override
    public void setSyntaxModel(XMLSyntaxModel syntaxModel) {
        setToRefNode(this.syntaxModel.refNode, syntaxModel);
    }

    @Override
    public XMLSyntaxModel getSyntaxModel() {
        return getFromRefNode(syntaxModel.refNode);
    }

    @Override
    public void setSubSyntaxModel(XMLSyntaxModel syntaxModel) {
        setToRefNode(this.subSyntaxModel.refNode, syntaxModel);
    }

    @Override
    public XMLSyntaxModel getSubSyntaxModel() {
        return getFromRefNode(subSyntaxModel.refNode);
    }

    @Override
    public void setXpath(String xpath) {
        this.xpath.setValue(xpath);
    }

    @Override
    public String getXpath() {
        return xpath.getValue();
    }

    @Override
    public void setTarget(String target) {
        this.target.setValue(target);
    }

    @Override
    public String getTarget() {
        return target.getValue();
    }

}
