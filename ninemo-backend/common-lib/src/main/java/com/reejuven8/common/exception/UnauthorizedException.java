package com.reejuven8.common.exception;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
