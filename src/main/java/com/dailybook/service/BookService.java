package com.dailybook.service;

import com.dailybook.model.Book;
import com.dailybook.model.Essay;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class BookService {

    private final OpenLibraryService openLibraryService;
    private final DeepSeekAiService deepSeekAiService;
    private final Random random = new Random();

    public BookService(OpenLibraryService openLibraryService, DeepSeekAiService deepSeekAiService) {
        this.openLibraryService = openLibraryService;
        this.deepSeekAiService = deepSeekAiService;
    }

    /**
     * Fetches top books from multiple categories
     */
    public List<Book> getTopBooks(List<String> categories) {
        return openLibraryService.fetchTopBooks(categories);
    }

    /**
     * Gets a random book from the list
     */
    public Book getRandomBook(List<Book> books) {
        return openLibraryService.getRandomBook(books);
    }

    /**
     * Generates an essay for a selected book
     */
    public Essay generateBookEssay(Book book) {
        return deepSeekAiService.generateEssay(book);
    }

    /**
     * Gets the complete workflow result
     */
    public Essay generateDailyEssay() {
        // Define default categories
        List<String> categories = List.of("history", "ai", "fiction", "non_fiction");

        // Fetch top books
        List<Book> allBooks = getTopBooks(categories);

        if (allBooks.isEmpty()) {
            throw new RuntimeException("No books found. Check API configuration.");
        }

        // Select a random book
        Book selectedBook = getRandomBook(allBooks);

        // Fetch full description from Open Library
        String description = openLibraryService.fetchDescription(selectedBook.getIndustryIdentifier());
        if (description != null && !description.isEmpty()) {
            selectedBook.setDescription(description);
        }

        // Generate essay
        return generateBookEssay(selectedBook);
    }

    /**
     * Gets book details by category and rank
     */
    public Book getBookByCategoryAndRank(String category, Integer rank) {
        List<Book> books = getTopBooks(List.of(category));
        return books.stream()
                .filter(book -> book.getCategory().equalsIgnoreCase(category) && book.getRank() == rank)
                .findFirst()
                .orElse(null);
    }
}
