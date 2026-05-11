package com.dailybook.model;

import java.time.LocalDateTime;

public class Essay {
    private String id;
    private Book selectedBook;
    private String title;
    private String content;
    private String category;
    private LocalDateTime generatedAt;
    private AppleMusicLink appleMusicLink;
    private Integer estimatedReadingTimeMinutes;

    public Essay() {}

    public Essay(String id, Book selectedBook, String title, String content, String category, LocalDateTime generatedAt, AppleMusicLink appleMusicLink, Integer estimatedReadingTimeMinutes) {
        this.id = id;
        this.selectedBook = selectedBook;
        this.title = title;
        this.content = content;
        this.category = category;
        this.generatedAt = generatedAt;
        this.appleMusicLink = appleMusicLink;
        this.estimatedReadingTimeMinutes = estimatedReadingTimeMinutes;
    }

    public String getId() { return id; }
    public Book getSelectedBook() { return selectedBook; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public AppleMusicLink getAppleMusicLink() { return appleMusicLink; }
    public Integer getEstimatedReadingTimeMinutes() { return estimatedReadingTimeMinutes; }

    public void setId(String id) { this.id = id; }
    public void setSelectedBook(Book selectedBook) { this.selectedBook = selectedBook; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public void setAppleMusicLink(AppleMusicLink appleMusicLink) { this.appleMusicLink = appleMusicLink; }
    public void setEstimatedReadingTimeMinutes(Integer estimatedReadingTimeMinutes) { this.estimatedReadingTimeMinutes = estimatedReadingTimeMinutes; }

    public static class AppleMusicLink {
        private String url;
        private String deepLink;
        private String formattedText;

        public AppleMusicLink() {}

        public AppleMusicLink(String url, String deepLink, String formattedText) {
            this.url = url;
            this.deepLink = deepLink;
            this.formattedText = formattedText;
        }

        public String getUrl() { return url; }
        public String getDeepLink() { return deepLink; }
        public String getFormattedText() { return formattedText; }
    }
}
