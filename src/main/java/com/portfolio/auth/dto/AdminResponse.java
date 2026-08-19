package com.portfolio.auth.dto;

import java.time.Instant;

/**
 * The authenticated admin's own profile — backs the Admin Panel topbar, which displays the
 * logged-in admin's email. Never exposes {@code passwordHash}.
 */
public record AdminResponse(Long id, String email, Instant lastLoginAt) {
}
