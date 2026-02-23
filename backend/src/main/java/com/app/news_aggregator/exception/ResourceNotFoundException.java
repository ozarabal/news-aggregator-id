package com.app.news_aggregator.exception;

/**
 * Exception untuk resource yang tidak ditemukan (404).
 * Contoh penggunaan: sourceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Source", id))
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " dengan ID " + id + " tidak ditemukan");
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}