package com.dailybook.service;

import com.dailybook.model.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GoogleBooksService {

    private static final Map<String, String> CATEGORY_TO_LIST = Map.of(
            "history", "bestseller_history",
            "ai", "bestseller_technology_and_engineering",
            "fiction", "bestseller_fiction",
            "non_fiction", "bestseller_advice_how_to_and_miscellaneous"
    );

    @Value("${google.books.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches top 10 books for specified categories from Google Books API
     */
    public List<Book> fetchTopBooks(List<String> categories) {
        List<Book> allBooks = new ArrayList<>();

        for (String category : categories) {
            String listId = CATEGORY_TO_LIST.getOrDefault(
                    category.toLowerCase().replace(" ", "_"),
                    "bestseller_fiction"
            );
            List<Book> books = fetchBooksForCategory(listId, category);
            allBooks.addAll(books);
        }

        // Sort by rank within each category
        return allBooks.stream()
                .sorted(Comparator.comparing(Book::getRank))
                .collect(Collectors.toList());
    }

    /**
     * Fetches books for a specific category from Google Books API
     */
  private List<Book> fetchBooksForCategory(String listId, String categoryName) {
        String url = String.format(
                "https://www.googleapis.com/books/v1/volumes?printType=books" +
                        "&maxResults=10" +
                        "&sortBy=newly_published" +
                        "&listId=%s&q=%s",
                listId,
                categoryName
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            return parseBooksFromResponse(response, categoryName);
        } catch (Exception e) {
            System.err.println("Error fetching books for category " + categoryName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parses JSON response from Google Books API into Book objects
     */
    private List<Book> parseBooksFromResponse(String response, String category) {
        List<Book> books = new ArrayList<>();

        try {
            // Simple JSON parsing without external library
            if (response != null && response.contains("items")) {
                String[] items = extractItems(response);
                for (int i = 0; i < items.length && i < 10; i++) {
                    books.add(parseBookFromItem(items[i], category, i + 1));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing books: " + e.getMessage());
        }

        return books;
    }

    /**
     * Extracts individual book items from the API response
     */
  private String[] extractItems(String response) {
        int itemsStart = response.indexOf("\"items\"");
        if (itemsStart == -1) return new String[0];

        int arrayStart = response.indexOf("[", itemsStart);
        if (arrayStart == -1) return new String[0];

        int arrayEnd = findMatchingBracket(response, arrayStart);
        String itemsContent = response.substring(arrayStart, arrayEnd + 1);

        // Split by top-level book objects
        List<String> items = new ArrayList<>();
        int braceCount = 0;
        int currentStart = -1;

        for (int i = 0; i < itemsContent.length(); i++) {
            char c = itemsContent.charAt(i);
            if (c == '{') {
                if (braceCount == 0) currentStart = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && currentStart != -1) {
                    items.add(itemsContent.substring(currentStart, i + 1));
                    currentStart = -1;
                }
            }
        }

        return items.toArray(new String[0]);
    }

    /**
     * Finds the matching closing bracket for an opening bracket
     */
    private int findMatchingBracket(String content, int startPos) {
        int bracketCount = 0;
        boolean inString = false;
        char quoteChar = 0;

        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);

            if (!inString) {
                if (c == '"' || c == '\'') {
                    inString = true;
                    quoteChar = c;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0) return i;
                }
            } else {
                if (c == quoteChar && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
        }

        return content.length() - 1;
    }

    /**
     * Parses a single book item JSON string into a Book object
     */
   private Book parseBookFromItem(String itemJson, String category, int rank) {
        try {
            String title = extractString(itemJson, "title");
            String author = extractAuthor(itemJson);
            String description = extractDescription(itemJson);
            String thumbnail = extractThumbnail(itemJson);
            String industryId = extractIndustryIdentifier(itemJson);
            String publishedDate = extractPublishedDate(itemJson);

            return new Book(null, title, author, description, thumbnail, industryId, publishedDate, category, rank);
        } catch (Exception e) {
            System.err.println("Error parsing book: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a string value from JSON
     */
    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";

        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf('"', valueStart);

        if (valueEnd == -1) return "";
        return json.substring(valueStart, valueEnd);
    }

    /**
     * Extracts author(s) from JSON
     */
    private String extractAuthor(String itemJson) {
        try {
            if (itemJson.contains("\"authors\"")) {
                int authorsStart = itemJson.indexOf("\"authors\"");
                int arrayStart = itemJson.indexOf("[", authorsStart);
                int arrayEnd = itemJson.indexOf("]", arrayStart);
                if (arrayStart != -1 && arrayEnd != -1) {
                    String authorsArray = itemJson.substring(arrayStart, arrayEnd + 1);
                    List<String> authors = new ArrayList<>();
                    String authorPattern = "\"(.*?)\"";
                    int pos = 0;
                    while ((pos = authorsArray.indexOf('"', pos + 1)) != -1) {
                        int endPos = authorsArray.indexOf('"', pos + 1);
                        if (endPos != -1) {
                            authors.add(authorsArray.substring(pos + 1, endPos));
                            pos = endPos + 1;
                        }
                    }
                    return String.join(", ", authors);
                }
            }
            return extractString(itemJson, "volumeInfo") + " author";
        } catch (Exception e) {
            return "Unknown Author";
        }
    }

    /**
     * Extracts book description from JSON
     */
    private String extractDescription(String itemJson) {
        String desc = extractString(itemJson, "description");
        return desc.isEmpty() ? "No description available." : desc;
    }

    /**
     * Extracts thumbnail URL from JSON
     */
    private String extractThumbnail(String itemJson) {
        String imageLinks = extractString(itemJson, "thumbnail");
        return imageLinks.isEmpty() ? "https://via.placeholder.com/128x196?text=No+Image" : imageLinks;
    }

    /**
     * Extracts industry identifier from JSON
     */
    private String extractIndustryIdentifier(String itemJson) {
        // Try to extract ISBN
        String isbnPattern = "\"identifier\"\\s*:\\s*\\{\"value\"\\s*:\\s*\"([^\"]+)\"";
        int start = itemJson.indexOf("\"industryIdentifiers\"");
        if (start != -1) {
            int nextStart = itemJson.indexOf("\"type\"", start);
            if (nextStart != -1) {
                int valueStart = itemJson.indexOf("\"value\"", nextStart);
                if (valueStart != -1) {
                    int valueColon = itemJson.indexOf(":", valueStart);
                    if (valueColon != -1) {
                        int quoteStart = itemJson.indexOf('"', valueColon + 1);
                        int quoteEnd = itemJson.indexOf('"', quoteStart + 1);
                        if (quoteEnd != -1) {
                            return itemJson.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            }
        }
        return "N/A";
    }

    /**
     * Extracts published date from JSON
     */
    private String extractPublishedDate(String itemJson) {
        return extractString(itemJson, "publishedDate");
    }

    /**
     * Gets a random book from the list
     */
    public Book getRandomBook(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return null;
        }
        return books.get(new Random().nextInt(books.size()));
    }
}
