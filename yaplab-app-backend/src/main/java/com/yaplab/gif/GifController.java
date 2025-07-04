package com.yaplab.gif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling GIF-related operations.
 * Provides endpoints for searching GIFs, fetching trending GIFs, getting categories,
 * and downloading GIFs.
 */
@RestController
@RequestMapping("/api/gifs")
public class GifController {

    private static final Logger logger = LoggerFactory.getLogger(GifController.class);

    /**
     * Constructor-based dependency injection of GifService and GifDownloadService.
     */
    private final GifService gifService;
    private final GifDownloadService gifDownloadService;

    public GifController(GifService gifService, GifDownloadService gifDownloadService) {
        this.gifService = gifService;
        this.gifDownloadService = gifDownloadService;
    }

    /**
     * Searches for GIFs based on the provided query.
     * @return ResponseEntity containing the search results or an error message.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchGifs(
            @RequestParam("q") String query, // Explicitly map "q" URL parameter
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") String pos) {
        try {
            return ResponseEntity.ok(gifService.searchGifs(query, limit, pos));
        } catch (Exception e) {
            logger.error("Error searching GIFs: query={}, limit={}, pos={}", query, limit, pos, e);
            return ResponseEntity.badRequest().body("Error searching GIFs: " + e.getMessage());
        }
    }

    /**
     * Fetches trending GIFs.
     * @return ResponseEntity containing the trending GIFs or an error message.
     */
    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingGifs(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") String pos) {
        try {
            return ResponseEntity.ok(gifService.getTrendingGifs(limit, pos));
        } catch (Exception e) {
            logger.error("Error fetching trending GIFs: limit={}, pos={}", limit, pos, e);
            return ResponseEntity.badRequest().body("Error fetching trending GIFs: " + e.getMessage());
        }
    }

    /**
     * Fetches categories of GIFs.
     * @return ResponseEntity containing the categories or an error message.
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(gifService.getCategories());
        } catch (Exception e) {
            logger.error("Error fetching categories", e);
            return ResponseEntity.badRequest().body("Error fetching categories: " + e.getMessage());
        }
    }

    /**
     * Downloads a GIF from the provided URL and stores it in the database.
     * @param gifUrl The URL of the GIF to download.
     * @param title The title of the GIF.
     * @param userId The ID of the user requesting the download.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/download")
    public ResponseEntity<?> downloadAndStoreGif(
            @RequestParam String gifUrl,
            @RequestParam String title,
            @RequestParam Long userId) {
        try {
            return ResponseEntity.ok(gifDownloadService.downloadAndStoreGif(gifUrl, title, userId));
        } catch (Exception e) {
            logger.error("Error downloading GIF: gifUrl={}, title={}, userId={}", gifUrl, title, userId, e);
            return ResponseEntity.badRequest().body("Error downloading GIF: " + e.getMessage());
        }
    }
}
