package com.reejuven8.common.exception;

/**
 * The caller is authenticated but does not own / may not access the target resource.
 * Distinct from {@link UnauthorizedException}, which means "no valid identity at all".
 */
public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
