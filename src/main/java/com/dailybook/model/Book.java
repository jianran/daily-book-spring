package com.dailybook.model;

import java.util.HashMap;
import java.util.Map;

public class Book {
    private String id;
    private String title;
    private String author;
    private String description;
    private String thumbnailUrl;
    private String industryIdentifier;
    private String publishedDate;
    private String category;
    private Integer rank;

    public Book() {}

    public Book(String id, String title, String author, String description, String thumbnailUrl, String industryIdentifier, String publishedDate, String category, Integer rank) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.industryIdentifier = industryIdentifier;
        this.publishedDate = publishedDate;
        this.category = category;
        this.rank = rank;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getIndustryIdentifier() { return industryIdentifier; }
    public String getPublishedDate() { return publishedDate; }
    public String getCategory() { return category; }
    public Integer getRank() { return rank; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setIndustryIdentifier(String industryIdentifier) { this.industryIdentifier = industryIdentifier; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
    public void setCategory(String category) { this.category = category; }
    public void setRank(Integer rank) { this.rank = rank; }

    public static class BookBuilder {
        private String id;
        private String title;
        private String author;
        private String description;
        private String thumbnailUrl;
        private String industryIdentifier;
        private String publishedDate;
        private String category;
        private Integer rank;

        public BookBuilder id(String id) { this.id = id; return this; }
        public BookBuilder title(String title) { this.title = title; return this; }
        public BookBuilder author(String author) { this.author = author; return this; }
        public BookBuilder description(String description) { this.description = description; return this; }
        public BookBuilder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public BookBuilder industryIdentifier(String industryIdentifier) { this.industryIdentifier = industryIdentifier; return this; }
        public BookBuilder publishedDate(String publishedDate) { this.publishedDate = publishedDate; return this; }
        public BookBuilder category(String category) { this.category = category; return this; }
        public BookBuilder rank(Integer rank) { this.rank = rank; return this; }

        public Book build() {
            return new Book(id, title, author, description, thumbnailUrl, industryIdentifier, publishedDate, category, rank);
        }
    }
}
