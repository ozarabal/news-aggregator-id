package com.app.news_aggregator.service;

import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.exception.DuplicateResourceException;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.Bookmark;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.repository.ArticleRepository;
import com.app.news_aggregator.repository.BookmarkRepository;
import com.app.news_aggregator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    /** Ambil semua artikel yang di-bookmark user, diurutkan dari terbaru. */
    public List<ArticleDto.Summary> getBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(b -> ArticleDto.Summary.from(b.getArticle()))
                .toList();
    }

    /** Tambah bookmark. Throws DuplicateResourceException jika sudah ada. */
    @Transactional
    public void addBookmark(Long userId, Long articleId) {
        if (bookmarkRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new DuplicateResourceException("Artikel sudah di-bookmark");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Artikel dengan ID " + articleId + " tidak ditemukan"));

        User user = userRepository.getReferenceById(userId);

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .article(article)
                .build();

        bookmarkRepository.save(bookmark);
        log.debug("Bookmark ditambahkan: userId={}, articleId={}", userId, articleId);
    }

    /** Hapus bookmark. Throws ResourceNotFoundException jika belum di-bookmark. */
    @Transactional
    public void removeBookmark(Long userId, Long articleId) {
        if (!bookmarkRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new ResourceNotFoundException("Bookmark tidak ditemukan");
        }
        bookmarkRepository.deleteByUserIdAndArticleId(userId, articleId);
        log.debug("Bookmark dihapus: userId={}, articleId={}", userId, articleId);
    }

    /** Cek apakah artikel sudah di-bookmark oleh user. */
    public boolean isBookmarked(Long userId, Long articleId) {
        return bookmarkRepository.existsByUserIdAndArticleId(userId, articleId);
    }
}
