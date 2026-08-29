package com.memoq.backend.controller;

import com.memoq.backend.dto.CategoryDto;
import com.memoq.backend.dto.CategoryUpsertRequest;
import com.memoq.backend.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getAll() {
        return categoryService.getAll();
    }

    /** Creates the category if {@code slug} doesn't exist yet, otherwise renames it. */
    @PutMapping("/{slug}")
    public CategoryDto upsert(@PathVariable String slug, @Valid @RequestBody CategoryUpsertRequest request) {
        return categoryService.upsert(slug, request);
    }

    /** 409s if any question still references this category (FK is ON DELETE RESTRICT). */
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        categoryService.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
