package com.reejuven8.common.exception;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND", resource + " not found with id: " + id);
    }
}
