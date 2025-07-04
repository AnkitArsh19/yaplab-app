package com.yaplab.gif;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class GifSearchResponse {
    
    @JsonProperty("results")
    private List<GifResult> results;
    
    @JsonProperty("next")
    private String next;
    
    // Constructors
    public GifSearchResponse() {}
    
    public GifSearchResponse(List<GifResult> results, String next) {
        this.results = results;
        this.next = next;
    }
    
    // Getters and Setters
    public List<GifResult> getResults() {
        return results;
    }
    
    public void setResults(List<GifResult> results) {
        this.results = results;
    }
    
    public String getNext() {
        return next;
    }
    
    public void setNext(String next) {
        this.next = next;
    }
    
    // Inner class for individual GIF result
    public static class GifResult {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("media_formats")
        private Map<String, MediaFormat> mediaFormats;
        
        @JsonProperty("created")
        private Double created;
        
        @JsonProperty("content_description")
        private String contentDescription;
        
        @JsonProperty("itemurl")
        private String itemUrl;
        
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("tags")
        private List<String> tags;
        
        @JsonProperty("flags")
        private List<String> flags;
        
        @JsonProperty("hasaudio")
        private Boolean hasAudio;
        
        // Constructors
        public GifResult() {}
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Map<String, MediaFormat> getMediaFormats() { return mediaFormats; }
        public void setMediaFormats(Map<String, MediaFormat> mediaFormats) { this.mediaFormats = mediaFormats; }
        
        public Double getCreated() { return created; }
        public void setCreated(Double created) { this.created = created; }
        
        public String getContentDescription() { return contentDescription; }
        public void setContentDescription(String contentDescription) { this.contentDescription = contentDescription; }
        
        public String getItemUrl() { return itemUrl; }
        public void setItemUrl(String itemUrl) { this.itemUrl = itemUrl; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public List<String> getFlags() { return flags; }
        public void setFlags(List<String> flags) { this.flags = flags; }
        
        public Boolean getHasAudio() { return hasAudio; }
        public void setHasAudio(Boolean hasAudio) { this.hasAudio = hasAudio; }
    }
    
    // Media format class
    public static class MediaFormat {
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("duration")
        private Double duration;
        
        @JsonProperty("preview")
        private String preview;
        
        @JsonProperty("dims")
        private List<Integer> dims;
        
        @JsonProperty("size")
        private Long size;
        
        // Constructors
        public MediaFormat() {}
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public Double getDuration() { return duration; }
        public void setDuration(Double duration) { this.duration = duration; }
        
        public String getPreview() { return preview; }
        public void setPreview(String preview) { this.preview = preview; }
        
        public List<Integer> getDims() { return dims; }
        public void setDims(List<Integer> dims) { this.dims = dims; }
        
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
    }
}
