package com.app.news_aggregator.service;

import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.DigestLog;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.repository.ArticleRepository;
import com.app.news_aggregator.repository.DigestLogRepository;
import com.app.news_aggregator.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DigestService menangani seluruh proses pengiriman email digest:
 *
 * 1. Ambil artikel terpopuler sesuai preferensi kategori user
 * 2. Kelompokkan artikel per kategori
 * 3. Render template Thymeleaf menjadi HTML string
 * 4. Kirim email via JavaMailSender (SMTP)
 * 5. Catat hasil pengiriman ke DigestLog
 * 6. Update lastDigestSentAt di User
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigestService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;     // Thymeleaf template engine
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final DigestLogRepository digestLogRepository;

    // Berapa artikel per kategori yang dikirim dalam digest
    private static final int ARTICLES_PER_CATEGORY = 3;
    // Ambil artikel dari berapa jam ke belakang
    private static final int LOOKBACK_HOURS = 24;

    @Value("${spring.mail.username:no-reply@newsagg.com}")
    private String senderEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Kirim digest ke satu user.
     * Dipanggil oleh DigestConsumer (Worker) saat consume pesan dari queue.
     *
     * Alur:
     * 1. Load user dari DB
     * 2. Cek apakah user memang perlu digest (belum terima hari ini, dll)
     * 3. Ambil artikel sesuai preferensi
     * 4. Render email HTML
     * 5. Kirim email
     * 6. Update user & catat log
     */
    @Transactional
    public void sendDigestToUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[DIGEST] User ID {} tidak ditemukan", userId);
            return;
        }

        // Cek apakah user memang perlu digest sekarang
        if (!user.isDueForDigest()) {
            log.debug("[DIGEST] User {} belum waktunya menerima digest, skip", user.getEmail());
            saveLog(user, DigestLog.DigestStatus.SKIPPED, 0, "Belum waktunya");
            return;
        }

        // Cek apakah user punya preferensi kategori
        if (user.getCategoryPreferences().isEmpty()) {
            log.warn("[DIGEST] User {} tidak punya preferensi kategori", user.getEmail());
            saveLog(user, DigestLog.DigestStatus.SKIPPED, 0, "Tidak ada preferensi kategori");
            return;
        }

        // ---- Step 1: Ambil artikel per kategori ----
        Map<String, List<Article>> articlesByCategory = fetchArticlesForUser(user);
        int totalArticles = articlesByCategory.values().stream()
                .mapToInt(List::size).sum();

        if (totalArticles == 0) {
            log.info("[DIGEST] Tidak ada artikel baru untuk user {}", user.getEmail());
            saveLog(user, DigestLog.DigestStatus.SKIPPED, 0, "Tidak ada artikel baru");
            return;
        }

        // ---- Step 2: Render template email ----
        String htmlContent = renderEmailTemplate(user, articlesByCategory, totalArticles);

        // ---- Step 3: Kirim email ----
        try {
            sendEmail(user.getEmail(), "📰 News Digest Anda - " + todayFormatted(), htmlContent);

            // ---- Step 4: Update user & catat log sukses ----
            user.setLastDigestSentAt(LocalDateTime.now());
            userRepository.save(user);
            saveLog(user, DigestLog.DigestStatus.SENT, totalArticles, null);

            log.info("[DIGEST] Email berhasil dikirim ke {} ({} artikel)", user.getEmail(), totalArticles);

        } catch (Exception e) {
            log.error("[DIGEST] Gagal kirim email ke {}: {}", user.getEmail(), e.getMessage());
            saveLog(user, DigestLog.DigestStatus.FAILED, 0, e.getMessage());
            throw new RuntimeException("Gagal kirim digest ke " + user.getEmail(), e);
        }
    }

    /**
     * Ambil artikel terpopuler dari 24 jam terakhir sesuai preferensi user.
     * Return: Map<kategori, List<artikel>> — dikelompokkan per kategori.
     */
    private Map<String, List<Article>> fetchArticlesForUser(User user) {
        LocalDateTime since = LocalDateTime.now().minusHours(LOOKBACK_HOURS);
        Map<String, List<Article>> result = new LinkedHashMap<>();

        for (String category : user.getCategoryPreferences()) {
            // Ambil artikel terpopuler (by view count) dari kategori ini
            List<Article> articles = articleRepository.findPopularByCategories(
                    List.of(category),
                    since,
                    PageRequest.of(0, ARTICLES_PER_CATEGORY)
            ).getContent();

            if (!articles.isEmpty()) {
                result.put(category, articles);
            }
        }

        return result;
    }

    /**
     * Render template Thymeleaf menjadi HTML string.
     *
     * Context adalah "model" yang dikirim ke template.
     * Variable di context bisa diakses di template dengan ${variableName}.
     */
    private String renderEmailTemplate(User user,
                                       Map<String, List<Article>> articlesByCategory,
                                       int totalArticles) {
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("articlesByCategory", articlesByCategory);
        context.setVariable("totalArticles", totalArticles);
        context.setVariable("digestDate", todayFormatted());
        context.setVariable("unsubscribeUrl",
                baseUrl + "/api/v1/digest/unsubscribe?token=" + user.getDigestUnsubscribeToken());

        // "email/digest" merujuk ke file templates/email/digest.html
        return templateEngine.process("email/digest", context);
    }

    /**
     * Kirim email HTML via JavaMailSender.
     *
     * MimeMessage mendukung HTML dan attachment.
     * MimeMessageHelper memudahkan setup recipient, subject, body.
     */
    private void sendEmail(String to, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, "News Aggregator");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = isHtml

        mailSender.send(message);
    }

    /**
     * Simpan log pengiriman ke database.
     */
    private void saveLog(User user, DigestLog.DigestStatus status,
                         int articlesCount, String errorMessage) {
        digestLogRepository.save(DigestLog.builder()
                .user(user)
                .status(status)
                .recipientEmail(user.getEmail())
                .articlesCount(articlesCount)
                .errorMessage(errorMessage)
                .build());
    }

    private String todayFormatted() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID")));
    }
}