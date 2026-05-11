package com.dailybook.service;

import com.dailybook.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OpenLibraryService {

    private static final String SUBJECTS_URL = "https://openlibrary.org/subjects/%s.json?limit=10";
    private static final String COVER_URL = "https://covers.openlibrary.org/b/id/%d-M.jpg";
    private static final String WORK_URL = "https://openlibrary.org%s.json";

    private static final Map<String, String> CATEGORY_MAP = Map.of(
            "history", "history",
            "ai", "artificial_intelligence",
            "fiction", "fiction",
            "non_fiction", "non-fiction"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String url = String.format(SUBJECTS_URL, subject);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode works = root.get("works");

            if (works != null && works.isArray()) {
                int rank = 0;
                for (JsonNode work : works) {
                    rank++;
                    if (rank > 10) break;

                    String title = work.has("title") ? work.get("title").asText() : "Unknown Title";
                    String author = extractFirstAuthor(work);
                    String description = "A work in the " + categoryName + " category.";
                    String thumbnail = extractCoverUrl(work);
                    String key = work.has("key") ? work.get("key").asText() : "";
                    String firstPublishYear = work.has("first_publish_year")
                            ? String.valueOf(work.get("first_publish_year").asInt())
                            : "";

                    books.add(new Book(null, title, author, description, thumbnail,
                            key, firstPublishYear, categoryName, rank));
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching books for category " + categoryName + ": " + e.getMessage());
        }
        return books;
    }

    private String extractFirstAuthor(JsonNode work) {
        if (work.has("authors") && work.get("authors").isArray() && work.get("authors").size() > 0) {
            JsonNode firstAuthor = work.get("authors").get(0);
            return firstAuthor.has("name") ? firstAuthor.get("name").asText() : "Unknown Author";
        }
        return "Unknown Author";
    }

    private String extractCoverUrl(JsonNode work) {
        if (work.has("cover_id") && work.get("cover_id").asInt() > 0) {
            return String.format(COVER_URL, work.get("cover_id").asInt());
        }
        return "https://via.placeholder.com/128x196?text=No+Cover";
    }

    public String fetchDescription(String workKey) {
        if (workKey == null || workKey.isEmpty()) return "No description available.";
        try {
            String url = String.format(WORK_URL, workKey);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("description")) {
                JsonNode desc = root.get("description");
                if (desc.isTextual()) return desc.asText();
                if (desc.isObject() && desc.has("value")) return desc.get("value").asText();
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
