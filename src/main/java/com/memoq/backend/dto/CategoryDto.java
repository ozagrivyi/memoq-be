package com.memoq.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryDto(UUID id, String name, String slug, Instant createdAt, Instant updatedAt) {}
