package com.memoq.backend.repository;

import com.memoq.backend.entity.Question;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class QuestionSpecifications {

    private QuestionSpecifications() {}

    private static Specification<Question> categoryIdEquals(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Question> questionTextContains(String search) {
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("questionText")), pattern);
    }

    public static Specification<Question> filter(UUID categoryId, String search) {
        List<Specification<Question>> specs = new ArrayList<>();
        if (categoryId != null) {
            specs.add(categoryIdEquals(categoryId));
        }
        if (StringUtils.hasText(search)) {
            specs.add(questionTextContains(search));
        }
        return specs.stream().reduce(Specification::and).orElse((root, query, cb) -> cb.conjunction());
    }
}
