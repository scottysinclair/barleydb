package com.smartstream.morf.api.core.entity;

public enum EntityState {
	LOADING,
	LOADED, //the entity was loaded from the back-end
	NOTLOADED, //the entity is not loaded, is a new entity if the key is null
	DELETED

}
