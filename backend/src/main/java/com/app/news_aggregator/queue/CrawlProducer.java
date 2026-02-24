package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import com.app.news_aggregator.model.Article;
import com.app.news_aggregator.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CrawlProducer bertanggung jawab untuk mengirim task/pesan ke RabbitMQ.
 *
 * Analogi: Producer adalah "pengirim surat" yang menulis surat dan
 * memasukkannya ke kotak pos (queue). Dia tidak tahu kapan surat
 * akan diproses atau siapa yang akan membacanya.
 *
 * Producer TIDAK melakukan crawl — dia hanya kirim instruksi ke queue.
 * Crawl dilakukan oleh Consumer (Worker) yang mengkonsumsi pesan dari queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlProducer {

    /**
     * RabbitTemplate adalah "pengirim" yang disediakan Spring untuk
     * berinteraksi dengan RabbitMQ (kirim pesan, dll).
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Kirim task crawl untuk SATU sumber ke queue.
     *
     * Method ini dipanggil oleh Scheduler untuk setiap sumber aktif.
     * Pesan yang dikirim berisi: sourceId, sourceName, sourceUrl.
     *
     * @param source Sumber RSS yang akan di-crawl
     */
    public void enqueueCrawlSource(Source source) {
        CrawlMessage.CrawlRssMessage message = CrawlMessage.CrawlRssMessage.builder()
                .sourceId(source.getId())
                .sourceName(source.getName())
                .sourceUrl(source.getUrl())
                .build();

        try {
            /**
             * convertAndSend(exchange, routingKey, message)
             * - exchange    : "newsagg.exchange" (main exchange kita)
             * - routingKey  : "crawl.rss" (akan di-route ke crawl.rss.queue)
             * - message     : object Java yang akan dikonversi ke JSON
             */
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_CRAWL,
                    message
            );

            log.debug("Task crawl berhasil di-enqueue untuk sumber: {} (ID: {})",
                    source.getName(), source.getId());

        } catch (Exception e) {
            log.error("Gagal enqueue task crawl untuk sumber {}: {}", source.getName(), e.getMessage());
            // Tidak re-throw agar Scheduler bisa lanjut ke sumber berikutnya
        }
    }

    /**
     * Kirim task crawl untuk BANYAK sumber sekaligus (batch enqueue).
     *
     * Lebih efisien daripada panggil enqueueCrawlSource() satu per satu
     * karena kita bisa log summary di akhir.
     *
     * @param sources List sumber yang akan di-enqueue
     */
    public void enqueueCrawlSources(List<Source> sources) {
        log.info("Mengirim {} task crawl ke queue...", sources.size());

        int successCount = 0;
        for (Source source : sources) {
            try {
                enqueueCrawlSource(source);
                successCount++;
            } catch (Exception e) {
                log.warn("Gagal enqueue sumber {}: {}", source.getName(), e.getMessage());
            }
        }

        log.info("Berhasil enqueue {}/{} task crawl ke RabbitMQ", successCount, sources.size());
    }

    /**
     * Kirim task scraping konten untuk artikel yang baru disimpan.
     *
     * Dipanggil oleh CrawlerService setelah artikel baru berhasil disimpan ke DB.
     * Consumer (ScrapeConsumer) yang kemudian akan scrape konten lengkapnya.
     *
     * @param article Artikel yang perlu di-scrape kontennya
     */
    public void enqueueScrapeArticle(Article article) {
        CrawlMessage.ScrapeArticleMessage message = CrawlMessage.ScrapeArticleMessage.builder()
                .articleId(article.getId())
                .articleUrl(article.getUrl())
                .articleTitle(article.getTitle())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_SCRAPE,
                    message
            );

            log.debug("Task scrape berhasil di-enqueue untuk artikel ID: {}", article.getId());

        } catch (Exception e) {
            log.error("Gagal enqueue task scrape untuk artikel {}: {}",
                    article.getId(), e.getMessage());
        }
    }

    /**
     * Kirim task scraping untuk banyak artikel sekaligus.
     * Dipanggil setelah batch insert artikel baru dari hasil crawl.
     */
    public void enqueueScrapeArticles(List<Article> articles) {
        log.info("Mengirim {} task scrape ke queue...", articles.size());

        for (Article article : articles) {
            enqueueScrapeArticle(article);
        }
    }
}