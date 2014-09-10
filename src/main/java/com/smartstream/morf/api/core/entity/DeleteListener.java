package com.smartstream.morf.api.core.entity;

public interface DeleteListener<T> {
    public void entityDeleted(T object);
}
