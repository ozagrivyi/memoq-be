package com.memoq.backend.ai;

import com.memoq.backend.config.CacheConfig;
import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.QuestionRequest;
import com.memoq.backend.entity.Category;
import com.memoq.backend.repository.CategoryRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns a question to a category by asking Claude to pick the best-fitting existing category or
 * suggest a new one, instead of the client (editor.html) choosing a categoryId directly.
 */
@Service
@RequiredArgsConstructor
public class CategoryInferenceService {

    private final ChatClient chatClient;
    private final CategoryRepository categoryRepository;
    private final CacheManager cacheManager;

    @Transactional
    public Category resolveCategory(QuestionRequest request) {
        List<Category> existing = categoryRepository.findAll();
        CategorySuggestion suggestion = suggestCategory(request, existing);

        return existing.stream()
                .filter(category -> category.getName().equalsIgnoreCase(suggestion.categoryName()))
                .findFirst()
                .orElseGet(() -> createCategory(suggestion.categoryName()));
    }

    private CategorySuggestion suggestCategory(QuestionRequest request, List<Category> existing) {
        String existingNames = existing.isEmpty()
                ? "(none yet)"
                : existing.stream().map(Category::getName).reduce((a, b) -> a + ", " + b).orElse("");

        String optionsList = request.options().stream()
                .map(QuestionOptionRequest::text)
                .collect(Collectors.joining(" | "));

        String prompt =
                """
                You are categorizing a quiz question for an IT/DevOps/networking certification-prep
                game.

                Existing categories: %s

                Question: %s
                Options: %s
                Explanation: %s

                Pick the single best-fitting category for this question. If one of the existing
                categories is a reasonable fit (even if the wording differs slightly), reuse its
                exact name exactly as given above. Only propose a new category name if none of the
                existing ones fit at all. A new name must be a short, general topic (1-3 words,
                Title Case) — not a restatement of the question.
                """
                        .formatted(existingNames, request.questionText(), optionsList, request.explanation());

        return chatClient.prompt().user(prompt).call().entity(CategorySuggestion.class);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(uniqueSlug(name));
        Category saved = categoryRepository.save(category);

        Cache cache = cacheManager.getCache(CacheConfig.CATEGORIES_CACHE);
        if (cache != null) {
            cache.clear();
        }
        return saved;
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) {
            base = "category";
        }
        String slug = base;
        int suffix = 2;
        while (categoryRepository.findBySlugIgnoreCase(slug).isPresent()) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}
