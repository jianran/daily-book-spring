package com.dailybook.service;

import com.dailybook.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OpenLibraryService {

    private static final String GOOGLE_BOOKS_URL = "https://www.googleapis.com/books/v1/volumes?q=subject:%s&maxResults=10&orderBy=newest";

    private static final Map<String, String> CATEGORY_MAP;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("history", "history");
        map.put("ai", "artificial-intelligence");
        map.put("fiction", "fiction");
        map.put("non_fiction", "nonfiction");
        map.put("bestseller_technology_and_engineering", "technology");
        map.put("bestseller_fiction", "fiction");
        map.put("bestseller_history", "history");
        map.put("bestseller_advice_how_to_and_miscellaneous", "self-help");
        map.put("bestseller_business", "business & money");
        map.put("bestseller_science", "science");
        map.put("bestseller_poetry", "poetry");
        map.put("trending_fiction", "fiction");
        map.put("trending_non_fiction", "nonfiction");
        CATEGORY_MAP = Map.copyOf(map);
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.books.api.key:}")
    private String apiKey;

    public List<Book> fetchTopBooks(List<String> categories) {
        List<Book> allBooks = new ArrayList<>();

        for (String category : categories) {
            String subject = CATEGORY_MAP.getOrDefault(
                    category.toLowerCase().replace(" ", "_"),
                    "fiction"
            );
            List<Book> books = fetchBooksBySubject(subject, category);
            allBooks.addAll(books);
        }

        return allBooks.stream()
                .sorted(Comparator.comparing(Book::getRank))
                .collect(Collectors.toList());
    }

    private List<Book> fetchBooksBySubject(String subject, String categoryName) {
        List<Book> books = new ArrayList<>();
        try {
            String url = String.format(GOOGLE_BOOKS_URL, subject)
                    + (apiKey != null && !apiKey.isEmpty() ? "&key=" + apiKey : "");
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                int rank = 0;
                for (JsonNode item : items) {
                    rank++;
                    if (rank > 10) break;

                    JsonNode volumeInfo = item.get("volumeInfo");
                    String title = hasText(volumeInfo, "title") ? volumeInfo.get("title").asText() : "Unknown Title";
                    List<String> authors = extractAuthors(volumeInfo);
                    String description = hasText(volumeInfo, "description") ? volumeInfo.get("description").asText() : "A book in the " + categoryName + " category.";
                    String thumbnail = extractThumbnail(volumeInfo);
                    String isbn = extractIsbn(volumeInfo);
                    String publishedDate = hasText(volumeInfo, "publishedDate") ? volumeInfo.get("publishedDate").asText() : "";

                    books.add(new Book(isbn, title, authors.isEmpty() ? "Unknown Author" : String.join(", ", authors),
                            description, thumbnail, isbn, publishedDate, categoryName, rank));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching books for category " + categoryName + ": " + e.getMessage());
        }
        return books;
    }

    private boolean hasText(JsonNode node, String field) {
        return node != null && node.has(field) && node.get(field).isTextual() && !node.get(field).asText().isEmpty();
    }

    private String extractThumbnail(JsonNode volumeInfo) {
        if (volumeInfo != null && volumeInfo.has("imageLinks") && volumeInfo.get("imageLinks").isObject()) {
            JsonNode thumb = volumeInfo.get("imageLinks").get("thumbnail");
            if (thumb != null && thumb.isTextual() && !thumb.asText().isEmpty()) {
                return thumb.asText();
            }
        }
        return "https://via.placeholder.com/128x196?text=No+Cover";
    }

    private List<String> extractAuthors(JsonNode volumeInfo) {
        List<String> authors = new ArrayList<>();
        if (volumeInfo != null && volumeInfo.has("authors") && volumeInfo.get("authors").isArray()) {
            for (JsonNode author : volumeInfo.get("authors")) {
                authors.add(author.asText());
            }
        }
        return authors;
    }

    private String extractIsbn(JsonNode volumeInfo) {
        if (volumeInfo != null && volumeInfo.has("industryIdentifiers") && volumeInfo.get("industryIdentifiers").isArray()) {
            for (JsonNode id : volumeInfo.get("industryIdentifiers")) {
                if ("ISBN_13".equals(id.get("type").asText())) {
                    return id.get("identifier").asText();
                }
            }
        }
        return "";
    }

    public String fetchDescription(String isbn) {
        if (isbn == null || isbn.isEmpty()) return "No description available.";
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn + "&maxResults=1";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");

            if (items != null && items.isArray() && items.size() > 0) {
                JsonNode desc = items.get(0).get("volumeInfo").get("description");
                if (desc != null && desc.isTextual()) return desc.asText();
            }
        } catch (Exception e) {
            System.err.println("Error fetching description: " + e.getMessage());
        }
        return "No description available.";
    }

    public Book getRandomBook(List<Book> books) {
        if (books == null || books.isEmpty()) return null;
        return books.get(new Random().nextInt(books.size()));
    }
}
