package com.app.news_aggregator.exception;

/**
 * Exception untuk resource yang sudah ada (409 Conflict).
 * Contoh penggunaan: jika URL RSS sudah terdaftar.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
