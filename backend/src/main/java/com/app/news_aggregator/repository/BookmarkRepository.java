package com.app.news_aggregator.repository;

import com.app.news_aggregator.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    Optional<Bookmark> findByUserIdAndArticleId(Long userId, Long articleId);

    void deleteByUserIdAndArticleId(Long userId, Long articleId);

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
}
