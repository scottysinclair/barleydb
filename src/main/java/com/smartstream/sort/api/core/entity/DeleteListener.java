package com.smartstream.sort.api.core.entity;

public interface DeleteListener<T> {
    public void entityDeleted(T object);
}
