package com.memoq.backend.repository;

import com.memoq.backend.entity.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findBySlugIgnoreCase(String slug);
}
