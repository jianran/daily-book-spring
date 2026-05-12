package com.dailybook.controller;

import com.dailybook.dto.BookEssayResponse;
import com.dailybook.model.Book;
import com.dailybook.model.Essay;
import com.dailybook.service.BookService;
import com.dailybook.service.DiscordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    private final DiscordService discordService;

    public BookController(BookService bookService, DiscordService discordService) {
        this.bookService = bookService;
        this.discordService = discordService;
    }

    /**
     * GET /api/books - Get top books from specified categories
     */
    @GetMapping
    public ResponseEntity<List<Book>> getTopBooks(
            @RequestParam(defaultValue = "history,ai,fiction,non_fiction") List<String> categories) {
        List<Book> books = bookService.getTopBooks(categories);
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/books/essay - Generate and return a daily book essay
     */
    @GetMapping("/essay")
    public ResponseEntity<BookEssayResponse> getDailyEssay(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer rank) {

        Essay essay;
        String message;

 if (category != null && rank != null) {
            // Get specific book and generate essay
            Book book = bookService.getBookByCategoryAndRank(category, rank);
            if (book == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new BookEssayResponse("error", null, String.format("Book not found for category '%s' with rank %d", category, rank)));
            }
            essay = bookService.generateBookEssay(book);
            message = String.format("Essay generated for '%s' by %s (Rank #%d in %s)",
                    book.getTitle(), book.getAuthor(), book.getRank(), book.getCategory());
        } else {
            // Generate essay for random book
            essay = bookService.generateDailyEssay();
            message = String.format("Daily essay generated for '%s' by %s",
                    essay.getSelectedBook().getTitle(), essay.getSelectedBook().getAuthor());
        }

        return ResponseEntity.ok(new BookEssayResponse("success", essay, message));
    }

    /**
     * GET /api/books/essay/{bookId} - Generate essay for a specific book
     */
  @GetMapping("/essay/{bookId}")
    public ResponseEntity<BookEssayResponse> getEssayForBook(@PathVariable String bookId) {
        // Note: In a real implementation, you would fetch the book by ID
        // For now, this will generate an essay using the daily selection
        Essay essay = bookService.generateDailyEssay();
        return ResponseEntity.ok(new BookEssayResponse("success", essay, "Essay generated successfully"));
    }

    /**
     * POST /api/books/essay - Generate essay with custom parameters
     */
    @PostMapping("/essay")
    public ResponseEntity<BookEssayResponse> generateCustomEssay(
            @RequestBody CustomEssayRequest request) {

        List<String> categories = request.getCategories();
        Essay essay = bookService.generateDailyEssay();

        return ResponseEntity.ok(new BookEssayResponse("success", essay, "Custom essay generated successfully"));
    }

    /**
     * GET /api/books/categories - Get available book categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAvailableCategories() {
        List<String> categories = List.of(
                "history",
                "ai",
                "fiction",
                "non_fiction",
                "bestseller_technology_and_engineering",
                "bestseller_fiction",
                "bestseller_history",
                "bestseller_advice_how_to_and_miscellaneous",
                "bestseller_business",
                "bestseller_science",
                "bestseller_poetry",
                "trending_fiction",
                "trending_non_fiction"
        );
        return ResponseEntity.ok(categories);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Daily Book API is running!");
    }

    /**
     * Generate essay and send to Discord DM
     */
    @PostMapping("/essay/discord")
    public ResponseEntity<BookEssayResponse> generateAndSendToDiscord() {
        Essay essay = bookService.generateDailyEssay();

        // Send to Discord
        discordService.sendEssayToDirectMessage(essay);

        return ResponseEntity.ok(new BookEssayResponse(
                "success",
                essay,
                "Essay generated and sent to Discord DM"
        ));
    }

   /**
     * Custom essay request DTO
     */
    public static class CustomEssayRequest {
        private List<String> categories;
        private String apiKey;
        private Integer numberOfBooksPerCategory;

        public CustomEssayRequest() {}

        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public Integer getNumberOfBooksPerCategory() { return numberOfBooksPerCategory; }
        public void setNumberOfBooksPerCategory(Integer numberOfBooksPerCategory) { this.numberOfBooksPerCategory = numberOfBooksPerCategory; }
    }
}
