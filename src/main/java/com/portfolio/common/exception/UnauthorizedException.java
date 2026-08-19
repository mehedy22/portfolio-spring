package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 401 UNAUTHORIZED — missing, invalid, expired, or superseded credentials/token.
 *
 * <p>Login failures always use the same message regardless of cause, so an attacker cannot
 * distinguish "no such email" from "wrong password" (docs/08-security/authentication-authorization.md).
 */
public class UnauthorizedException extends ApiException {

	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
	}
}
