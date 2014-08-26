package com.smartstream.messaging.model;

import com.smartstream.mac.model.User;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ValueNode;
import com.smartstream.morf.api.core.proxy.AbstractCustomEntityProxy;
import com.smartstream.morf.api.core.proxy.RefNodeProxyHelper;

public abstract class SyntaxModel extends AbstractCustomEntityProxy  {
    private static final long serialVersionUID = 1L;
    private final ValueNode id;
    private final ValueNode name;
    private final ValueNode syntaxType;
    private final RefNodeProxyHelper user;

    public SyntaxModel(Entity entity) {
        super(entity);
        id = entity.getChild("id", ValueNode.class, true);
        name = entity.getChild("name", ValueNode.class, true);
        syntaxType = entity.getChild("syntaxType", ValueNode.class, true);
        user = new RefNodeProxyHelper(entity.getChild("user", RefNode.class, true));
    }

    public Long getId() {
        return id.getValue();
    }

    public String getName() {
        return name.getValue();
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public SyntaxType getSyntaxType() {
        return syntaxType.getValue();
    }

    public void setSyntaxType(SyntaxType syntaxType) {
        this.syntaxType.setValue(syntaxType);
    }

    public User getUser() {
        return super.getFromRefNode(user.refNode);
    }

    public void setUser(User user) {
        setToRefNode(this.user.refNode, user);
    }
}

