package com.reejuven8.common.exception;

/**
 * The request conflicts with existing state (e.g. creating a second active pregnancy
 * profile while one is already open).
 */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
