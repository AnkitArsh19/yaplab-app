package com.yaplab.gif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Service class for interacting with the Tenor GIF API.
 * Provides methods to search for GIFs, get trending GIFs, and fetch categories.
 */
@Service
public class GifService {

    private static final Logger logger = LoggerFactory.getLogger(GifService.class);

    // Tenor API configuration properties
    @Value("${tenor.api.key}")
    private String tenorApiKey;

    // Default API URL and client key, can be overridden in application properties
    @Value("${tenor.api.url:https://tenor.googleapis.com/v2}")
    private String tenorApiUrl;

    // Client key for Tenor, can be overridden in application properties
    @Value("${tenor.client.key:yaplab}")
    private String tenorClientKey;

    private final RestTemplate restTemplate;

    public GifService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Searches for GIFs using the Tenor API.
     * @param query The search query for GIFs.
     * @param limit The maximum number of GIFs to return.
     * @param pos   The position in the result set to start from (optional).
     * @return A map containing the search results.
     */
    public Map<String, Object> searchGifs(String query, int limit, String pos) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(tenorApiUrl + "/search")
                .queryParam("key", tenorApiKey)
                .queryParam("q", query)
                .queryParam("limit", limit)
                .queryParam("media_filter", "gif")
                .queryParam("ar_range", "all")
                .queryParam("client_key", tenorClientKey);

        if (pos != null && !pos.trim().isEmpty() && !"0".equals(pos.trim())) {
            builder.queryParam("pos", pos);
        }

        String url = builder.build().toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error searching GIFs from Tenor. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Fetches trending GIFs from the Tenor API.
     * @param limit The maximum number of GIFs to return.
     * @param pos   The position in the result set to start from (optional).
     * @return A map containing the trending GIFs.
     */
    public Map<String, Object> getTrendingGifs(int limit, String pos) {
        String pathSegment = "/featured";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(tenorApiUrl + pathSegment)
                .queryParam("key", tenorApiKey)
                .queryParam("client_key", tenorClientKey)
                .queryParam("limit", limit)
                .queryParam("media_filter", "minimal") // Use 'minimal' for best performance, or 'gif' for full URLs
                .queryParam("ar_range", "all")
                .queryParam("locale", "en");

        if (pos != null && !pos.trim().isEmpty() && !"0".equals(pos.trim())) {
            builder.queryParam("pos", pos);
        }

        String url = builder.build().toUriString();

        try {
            // Suppress unchecked warning for casting response to Map
            // It is used to prevent the compiler from showing warnings about unchecked type casts, which are sometimes unavoidable when working with generic types and legacy API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching trending GIFs from Tenor. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Fetches GIF categories from the Tenor API.
     * @return A map containing the categories of GIFs.
     */
    public Map<String, Object> getCategories() {
        String url = UriComponentsBuilder.fromUriString(tenorApiUrl + "/categories")
                .queryParam("key", tenorApiKey)
                .queryParam("type", "featured")
                .build()
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching GIF categories from Tenor. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
