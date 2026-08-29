package com.memoq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for PUT /api/v1/categories/{slug} — slug is the path key, this only carries the name. */
public record CategoryUpsertRequest(@NotBlank @Size(max = 120) String name) {}
