package com.memoq.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROMPT.md's domain model has no users table (single-editor tool), so auth is a single
 * configured admin identity rather than a user store. passwordHash must be a BCrypt hash.
 */
@ConfigurationProperties(prefix = "security.admin")
public record AdminProperties(String username, String passwordHash) {}
