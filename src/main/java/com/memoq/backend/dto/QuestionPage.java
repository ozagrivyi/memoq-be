package com.memoq.backend.dto;

import java.util.List;

/**
 * Cache-safe stand-in for {@code Page<QuestionDto>}: Spring Data's PageImpl has no default
 * constructor/setters, so it doesn't round-trip through Jackson cleanly. QuestionController
 * rebuilds a real Page/PagedModel from this after it comes out of the cache.
 */
public record QuestionPage(List<QuestionDto> content, long totalElements) {}
