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
 * RabbitMQConfig — topologi messaging lengkap.
 *
 * Queue yang tersedia:
 *
 *  newsagg.exchange (Direct)
 *    ├── crawl.rss       → crawl.rss.queue       (crawl RSS feed)
 *    ├── scrape.article  → scrape.article.queue   (scrape konten artikel)
 *    └── email.digest    → email.digest.queue     (kirim email digest) ← NEW Phase 5
 *
 *  dead.letter.exchange
 *    └── #               → dead.letter.queue      (pesan yang gagal)
 */
@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String EXCHANGE            = "newsagg.exchange";
    public static final String EXCHANGE_DEAD_LETTER = "dead.letter.exchange";

    // Queue & routing key — Crawl
    public static final String QUEUE_CRAWL_RSS      = "crawl.rss.queue";
    public static final String ROUTING_KEY_CRAWL    = "crawl.rss";

    // Queue & routing key — Scrape
    public static final String QUEUE_SCRAPE_ARTICLE  = "scrape.article.queue";
    public static final String ROUTING_KEY_SCRAPE    = "scrape.article";

    // Queue & routing key — Email Digest (NEW)
    public static final String QUEUE_EMAIL_DIGEST    = "email.digest.queue";
    public static final String ROUTING_KEY_DIGEST    = "email.digest";

    // Dead Letter Queue
    public static final String QUEUE_DEAD_LETTER     = "dead.letter.queue";

    // =============================================
    // EXCHANGE
    // =============================================
    @Bean public DirectExchange mainExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEAD_LETTER, true, false);
    }

    // =============================================
    // QUEUES
    // =============================================
    @Bean
    public Queue crawlRssQueue() {
        return QueueBuilder.durable(QUEUE_CRAWL_RSS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.crawl.rss")
                .withArgument("x-message-ttl", 3_600_000)
                .build();
    }

    @Bean
    public Queue scrapeArticleQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_ARTICLE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.scrape.article")
                .withArgument("x-message-ttl", 3_600_000)
                .build();
    }

    /**
     * Queue email digest.
     * TTL lebih pendek (30 menit) — kalau belum terkirim dalam 30 menit
     * berarti ada masalah yang perlu diinvestigasi, masukkan ke DLQ.
     */
    @Bean
    public Queue emailDigestQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL_DIGEST)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", "dead.email.digest")
                .withArgument("x-message-ttl", 1_800_000) // 30 menit
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DEAD_LETTER).build();
    }

    // =============================================
    // BINDINGS
    // =============================================
    @Bean public Binding bindingCrawlRss() {
        return BindingBuilder.bind(crawlRssQueue()).to(mainExchange()).with(ROUTING_KEY_CRAWL);
    }

    @Bean public Binding bindingScrapeArticle() {
        return BindingBuilder.bind(scrapeArticleQueue()).to(mainExchange()).with(ROUTING_KEY_SCRAPE);
    }

    @Bean public Binding bindingEmailDigest() {
        return BindingBuilder.bind(emailDigestQueue()).to(mainExchange()).with(ROUTING_KEY_DIGEST);
    }

    @Bean public Binding bindingDeadLetter() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }

    // =============================================
    // CONVERTER & TEMPLATE
    // =============================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

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

