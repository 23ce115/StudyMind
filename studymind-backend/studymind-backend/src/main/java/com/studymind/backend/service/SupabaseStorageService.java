package com.studymind.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Keeps the original uploaded file in a private Supabase Storage bucket (optional). Server-side only. */
@Service
public class SupabaseStorageService {
    private final RestClient http;
    private final String url;
    private final String key;
    private final String bucket;

    public SupabaseStorageService(RestClient http, @Value("${supabase.url}") String url,
                                  @Value("${supabase.service-role-key}") String key, @Value("${supabase.storage-bucket}") String bucket) {
        this.http = http;
        this.url = url;
        this.key = key;
        this.bucket = bucket;
    }

    public boolean isConfigured() { return !url.isBlank() && !key.isBlank(); }

    public void upload(String path, byte[] bytes, String contentType) {
        http.post().uri(url + "/storage/v1/object/" + bucket + "/" + path)
            .header("Authorization", "Bearer " + key).header("apikey", key).header("x-upsert", "true")
            .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
            .body(bytes).retrieve().toBodilessEntity();
    }

    public void delete(String path) {
        http.delete().uri(url + "/storage/v1/object/" + bucket + "/" + path)
            .header("Authorization", "Bearer " + key).header("apikey", key).retrieve().toBodilessEntity();
    }
}
