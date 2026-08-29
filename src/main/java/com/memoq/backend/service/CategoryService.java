package com.memoq.backend.service;

import com.memoq.backend.config.CacheConfig;
import com.memoq.backend.dto.CategoryDto;
import com.memoq.backend.dto.CategoryUpsertRequest;
import com.memoq.backend.dto.DtoMapper;
import com.memoq.backend.entity.Category;
import com.memoq.backend.exception.ConflictException;
import com.memoq.backend.exception.ResourceNotFoundException;
import com.memoq.backend.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(cacheNames = CacheConfig.CATEGORIES_CACHE, key = "'all'")
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(DtoMapper::toDto).toList();
    }

    /** Creates the category at {@code slug} if absent, otherwise renames it. Idempotent by design. */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.CATEGORIES_CACHE, allEntries = true)
    public CategoryDto upsert(String slug, CategoryUpsertRequest request) {
        categoryRepository
                .findByNameIgnoreCase(request.name())
                .filter(existing -> !existing.getSlug().equalsIgnoreCase(slug))
                .ifPresent(existing -> {
                    throw new ConflictException("A category named '" + request.name() + "' already exists.");
                });

        Category category = categoryRepository.findBySlugIgnoreCase(slug).orElseGet(Category::new);
        category.setSlug(slug);
        category.setName(request.name());
        return DtoMapper.toDto(categoryRepository.saveAndFlush(category));
    }

    /**
     * Fails with a 409 (via GlobalExceptionHandler's DataIntegrityViolationException handler) if
     * any question still references this category — questions.category_id is ON DELETE RESTRICT.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.CATEGORIES_CACHE, allEntries = true)
    public void delete(String slug) {
        Category category = categoryRepository
                .findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category '" + slug + "' not found."));
        categoryRepository.delete(category);
    }
}
