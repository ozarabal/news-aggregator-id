package com.app.news_aggregator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQConfig mendefinisikan seluruh topologi messaging:
 *
 * Arsitektur Queue kita:
 *
 *                         ┌─────────────────────────────────────────┐
 *                         │           EXCHANGE: newsagg.exchange     │
 *                         │            (type: Direct Exchange)       │
 *                         └──────────────────┬──────────────────────┘
 *                                            │
 *                    ┌──────────────────────┼──────────────────────┐
 *                    │ routing key:          │ routing key:         │
 *                    │ "crawl.rss"           │ "scrape.article"     │
 *                    ▼                       ▼                       
 *           ┌────────────────┐   ┌──────────────────────┐          
 *           │ crawl.rss.queue│   │ scrape.article.queue  │          
 *           │  (crawl RSS)   │   │  (scrape konten)      │          
 *           └───────┬────────┘   └──────────┬────────────┘          
 *                   │ jika gagal             │ jika gagal
 *                   ▼                        ▼
 *           ┌───────────────────────────────────────┐
 *           │      dead.letter.queue (DLQ)          │
 *           │  (pesan yang terus gagal diproses)    │
 *           └───────────────────────────────────────┘
 *
 * Direct Exchange: pesan dikirim ke queue berdasarkan exact match routing key.
 * Alternatif: Topic Exchange (wildcard), Fanout Exchange (broadcast ke semua queue).
 */
@Configuration
public class RabbitMQConfig {

    // =============================================
    // NAMA EXCHANGE & QUEUE (konstanta)
    // =============================================
    public static final String EXCHANGE = "newsagg.exchange";

    // Queue untuk crawl RSS feed
    public static final String QUEUE_CRAWL_RSS = "crawl.rss.queue";
    public static final String ROUTING_KEY_CRAWL = "crawl.rss";

    // Queue untuk scraping konten artikel
    public static final String QUEUE_SCRAPE_ARTICLE = "scrape.article.queue";
    public static final String ROUTING_KEY_SCRAPE = "scrape.article";

    // Dead Letter Queue — tampung pesan yang gagal diproses berkali-kali
    public static final String QUEUE_DEAD_LETTER = "dead.letter.queue";
    public static final String EXCHANGE_DEAD_LETTER = "dead.letter.exchange";

    // =============================================
    // EXCHANGE SETUP
    // =============================================

    /**
     * Main Exchange: Direct Exchange.
     * durable=true: exchange tidak hilang jika RabbitMQ restart.
     */
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * Dead Letter Exchange: menerima pesan yang di-reject atau expired.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEAD_LETTER, true, false);
    }

    // =============================================
    // QUEUE SETUP
    // =============================================

    /**
     * Queue untuk crawl RSS.
     *
     * Argumen penting:
     * - x-dead-letter-exchange: jika pesan gagal diproses (NACK/expired),
     *   kirim ke exchange ini
     * - x-message-ttl: pesan expired setelah 1 jam (3_600_000 ms)
     *   mencegah queue penuh dengan pesan lama yang tidak terproses
     */
    @Bean
    public Queue crawlRssQueue() {
        return QueueBuilder.durable(QUEUE_CRAWL_RSS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.crawl.rss")
                .withArgument("x-message-ttl", 3_600_000) // 1 jam
                .build();
    }

    /**
     * Queue untuk scrape artikel.
     * Sama seperti crawlRssQueue tapi untuk task scraping.
     */
    @Bean
    public Queue scrapeArticleQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_ARTICLE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.scrape.article")
                .withArgument("x-message-ttl", 3_600_000)
                .build();
    }

    /**
     * Dead Letter Queue: menampung pesan yang gagal diproses.
     * Tidak ada DLX lagi dari sini (tidak infinite loop).
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DEAD_LETTER).build();
    }

    // =============================================
    // BINDING — Menghubungkan Exchange ke Queue
    // =============================================

    /**
     * Binding: mainExchange → crawlRssQueue via routing key "crawl.rss"
     * Artinya: pesan yang dikirim ke mainExchange dengan routing key "crawl.rss"
     * akan diteruskan ke crawlRssQueue.
     */
    @Bean
    public Binding bindingCrawlRss() {
        return BindingBuilder
                .bind(crawlRssQueue())
                .to(mainExchange())
                .with(ROUTING_KEY_CRAWL);
    }

    @Bean
    public Binding bindingScrapeArticle() {
        return BindingBuilder
                .bind(scrapeArticleQueue())
                .to(mainExchange())
                .with(ROUTING_KEY_SCRAPE);
    }

    @Bean
    public Binding bindingDeadLetter() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#"); // wildcard: semua routing key masuk ke DLQ
    }

    // =============================================
    // MESSAGE CONVERTER
    // =============================================

    /**
     * MessageConverter: konversi object Java ↔ JSON saat kirim/terima pesan.
     * Tanpa ini, RabbitMQ memakai Java serialization (format binary, tidak readable).
     * Dengan Jackson2JsonMessageConverter, pesan dikirim sebagai JSON yang bisa dibaca.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate: dipakai oleh Producer untuk kirim pesan.
     * Set converter ke JSON agar pesan terkirim dalam format JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Listener Container Factory: konfigurasi untuk Consumer/Worker.
     *
     * prefetchCount=1: Worker hanya ambil 1 pesan dari queue sekaligus.
     * Ini penting agar distribusi kerja merata jika ada banyak worker.
     * Tanpa ini (default prefetch tidak terbatas), worker pertama bisa
     * mengambil semua pesan sekaligus meskipun sibuk.
     *
     * acknowledgeMode=MANUAL: Worker harus explicit ACK/NACK setelah selesai.
     * Jika worker crash sebelum ACK, pesan akan dikembalikan ke queue.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}