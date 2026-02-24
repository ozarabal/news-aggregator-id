package com.app.news_aggregator.queue;

import com.app.news_aggregator.config.RabbitMQConfig;
import com.app.news_aggregator.service.CrawlerService;
import com.app.news_aggregator.exception.ResourceNotFoundException;
import com.app.news_aggregator.model.Source;
import com.app.news_aggregator.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CrawlConsumer adalah Worker yang mengkonsumsi pesan dari queue crawl.rss.queue.
 *
 * Analogi: Consumer adalah "tukang pos" yang mengambil surat dari kotak pos
 * dan mengantarkannya ke tujuan (memproses task crawl).
 *
 * @RabbitListener secara otomatis:
 * 1. Membuka koneksi ke RabbitMQ
 * 2. Subscribe ke queue yang ditentukan
 * 3. Memanggil method saat ada pesan masuk
 * 4. ACK otomatis jika method selesai tanpa exception
 * 5. NACK otomatis jika method throw exception (pesan akan di-retry atau ke DLQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlConsumer {

    private final CrawlerService crawlerService;
    private final SourceRepository sourceRepository;

    /**
     * Listen ke crawl.rss.queue dan proses setiap pesan yang masuk.
     *
     * Spring otomatis deserilisasi JSON pesan ke object CrawlRssMessage.
     *
     * Alur:
     * 1. Pesan masuk dari queue
     * 2. Spring deserilisasi JSON → CrawlRssMessage
     * 3. Method ini dipanggil dengan object tersebut
     * 4. Ambil Source dari DB berdasarkan sourceId
     * 5. Jalankan crawl via CrawlerService
     * 6. Jika sukses → ACK otomatis (pesan dihapus dari queue)
     * 7. Jika exception → NACK (pesan dikembalikan / ke DLQ)
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CRAWL_RSS)
    public void consumeCrawlRss(CrawlMessage.CrawlRssMessage message) {
        log.info("[WORKER] Menerima task crawl: sumber '{}' (ID: {})",
                message.getSourceName(), message.getSourceId());

        try {
            // Ambil Source dari database
            Source source = sourceRepository.findById(message.getSourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source", message.getSourceId()));

            // Jalankan crawl secara SINKRON di dalam consumer
            // Consumer sudah berjalan di thread terpisah (thread pool RabbitMQ listener)
            // jadi tidak perlu @Async lagi
            crawlerService.crawlSource(source);

            log.info("[WORKER] Task crawl selesai untuk sumber: '{}'", message.getSourceName());

        } catch (ResourceNotFoundException e) {
            // Source tidak ditemukan (mungkin sudah dihapus)
            // Tidak perlu retry — log warning dan biarkan pesan di-ACK
            log.warn("[WORKER] Sumber ID {} tidak ditemukan, task diabaikan: {}",
                    message.getSourceId(), e.getMessage());
            // Tidak throw exception → Spring akan ACK pesan (tidak retry)

        } catch (Exception e) {
            // Error lain → throw agar Spring NACK dan pesan masuk DLQ
            log.error("[WORKER] Gagal proses task crawl untuk sumber '{}': {}",
                    message.getSourceName(), e.getMessage());
            throw new RuntimeException("Gagal crawl sumber: " + message.getSourceName(), e);
        }
    }
}