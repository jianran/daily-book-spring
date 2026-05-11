package com.dailybook.service;

import com.dailybook.model.Book;
import com.dailybook.model.Essay;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public DeepSeekAiService(
            @Value("${spring.ai.deepseek.api.key:}") String apiKey,
            @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}") String model,
            @Value("${spring.ai.deepseek.chat.options.temperature:0.7}") double temperature) {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
    }

   /**
     * Generates a 10-minute essay about a book using DeepSeek AI
     */
   public Essay generateEssay(Book selectedBook) {
        String essayContent = generateEssayContent(selectedBook);

        Essay.AppleMusicLink musicLink = createAppleMusicLink(selectedBook);

        return new Essay(
                "essay_" + System.currentTimeMillis(),
                selectedBook,
                "A " + getReadingTimeDescription() + " Essay: " + selectedBook.getTitle(),
                essayContent,
                selectedBook.getCategory(),
                LocalDateTime.now(),
                musicLink,
                getEstimatedReadingTime()
        );
    }

   /**
     * Generates the essay content using DeepSeek AI
     */
    private String generateEssayContent(Book book) {
        String prompt = buildEssayPrompt(book);

        try {
            // Call DeepSeek API directly via WebClient
            String response = webClient.post()
                    .uri("https://api.deepseek.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of("role", "user", "content", prompt)
                            ),
                            "temperature", temperature,
                            "max_tokens", 2000
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response to extract the essay content
            if (response != null) {
                String content = extractContentFromResponse(response);
                return content != null ? content : generateFallbackEssay(book);
            }
            return generateFallbackEssay(book);

        } catch (Exception e) {
            System.err.println("Error generating essay: " + e.getMessage());
            return generateFallbackEssay(book);
        }
    }

    /**
     * Extracts content from DeepSeek API JSON response using Jackson for safe parsing.
     */
    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // Simple implementation - would use a JSON library in production
        return new HashMap<>();
    }

    /**
     * Builds the prompt for essay generation
     */
    private String buildEssayPrompt(Book book) {
        return String.format("""
                You are an expert essay writer and literary critic. Your task is to write
                a comprehensive 10-minute reading essay (approximately 1500-1800 words) about
                the following book:

                TITLE: %s
                AUTHOR: %s
                CATEGORY: %s
                PUBLISHED: %s
                DESCRIPTION: %s

                Write an engaging, well-structured essay that includes:

                1. INTRODUCTION (200-250 words)
                   - Hook the reader with an interesting opening
                   - Introduce the book and its author
                   - Set the context and significance of the work
                   - Present your main thesis about the book

                2. BACKGROUND & CONTEXT (250-300 words)
                   - Discuss the author's background and writing style
                   - Explain the historical or cultural context
                   - Mention any relevant themes or influences
                   - Why was this book written and what gap does it fill?

                3. THEMATIC ANALYSIS (400-500 words)
                   - Deep dive into 2-3 major themes
                   - Analyze how these themes are developed
                   - Discuss the book's message and its relevance
                   - Provide specific examples from the text

                4. CRITICAL EVALUATION (300-400 words)
                   - Strengths and unique contributions
                   - Notable writing techniques or approaches
                   - Comparisons to similar works (if applicable)
                   - The book's place in its genre or category

                5. CONCLUSION & RECOMMENDATION (200-250 words)
                   - Summarize key insights
                   - Who should read this book and why
                   - Final thoughts on its lasting impact
                   - A compelling closing statement

                STYLE GUIDELINES:
                - Write in a sophisticated but accessible tone
                - Use varied sentence structures for engagement
                - Include smooth transitions between paragraphs
                - Maintain objectivity while offering insights
                - Avoid spoilers if discussing specific plot elements
                - Use proper paragraph breaks and structure

                IMPORTANT: Write a continuous essay, not bullet points.
                The essay should read like professional book criticism
                that would appear in a quality publication.

                Begin your essay now:
                """,
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getPublishedDate(),
                book.getDescription()
        );
    }

    /**
     * Fallback essay generation when API fails
     */
    private String generateFallbackEssay(Book book) {
        return String.format("""
                A DECADE-LONG EXPLORATION: UNDERSTANDING %s BY %s

                INTRODUCTION

                In the vast landscape of literature, certain books emerge that not only define
                their genre but also offer profound insights into the human experience. "%s"
                by %s represents such a work, standing as a testament to the enduring power
                of thoughtful writing and compelling storytelling.

                This essay will explore the multifaceted nature of this remarkable work,
                examining its themes, context, and lasting significance. Through careful
                analysis, we will uncover what makes this book worthy of attention and
                why it continues to resonate with readers.

                BACKGROUND AND CONTEXT

                To fully appreciate "%s", one must understand the context in which it was
                created. %s, the author, has crafted a work that reflects both personal
                vision and broader cultural currents. The publication of this book in %s
                marked an important moment in the %s category, bringing fresh perspectives
                to an already vibrant field.

                The author's approach to this subject matter demonstrates a deep understanding
                of the topic, combined with an ability to present complex ideas in accessible
                ways. This skill is essential for works that aim to educate while entertaining,
                and %s has mastered this balance.

                THEMATIC ANALYSIS

                Several key themes emerge throughout "%s", each contributing to the book's
                overall impact and appeal.

                First, the theme of innovation and progress stands out prominently. Whether
                exploring artificial intelligence, historical patterns, or narrative structures,
                the book consistently examines how ideas evolve and transform over time.
                This thematic focus gives the work its intellectual depth and contemporary
                relevance.

                Second, the exploration of human nature and motivation provides a rich
                foundation for understanding the book's characters and arguments. The author's
                ability to weave together individual stories with larger patterns creates a
                compelling narrative that keeps readers engaged.

                Third, the work examines the tension between tradition and innovation, a
                theme that resonates across multiple disciplines. This examination helps
                readers understand both the importance of learning from the past and the
                necessity of embracing new possibilities.

                CRITICAL EVALUATION

                What sets "%s" apart from similar works is its exceptional clarity of thought
                and writing. The author has succeeded in creating a book that is both
                intellectually rigorous and accessible to a broad audience. This achievement
                is no small feat and speaks to the author's mastery of the craft.

                The book's strength lies in its comprehensive approach. Rather than offering
                superficial treatment of its topics, "%s" dives deep into meaningful analysis.
                This depth, combined with clear writing, makes it valuable both for those
                new to the subject and for experienced readers seeking fresh insights.

                One notable strength is the book's ability to connect abstract concepts to
                concrete examples. This pedagogical approach helps readers grasp complex
                ideas and see their practical applications. The author's skill in this area
                makes the book an excellent choice for both casual reading and serious study.

                CONCLUSION AND RECOMMENDATION

                "%s" by %s is a significant work that deserves attention from anyone interested
                in %s. Its thoughtful analysis, clear writing, and engaging presentation make
                it a standout contribution to the field.

                The book's lasting value lies in its ability to inform while inspiring.
                Readers will finish this work not only with new knowledge but also with
                a deeper appreciation for the complexity and beauty of its subject matter.

                For those seeking a substantial, thought-provoking read that will expand
                their understanding, "%s" is highly recommended. Whether read for pleasure
                or study, this book offers rewards that extend well beyond the final page.

                In an era of information overload, "%s" stands as a beacon of quality,
                offering substance over sensation and depth over superficiality. It is a
                reminder of what great writing can achieve when an author is truly committed
                to their craft.
                """,
                book.getTitle().toUpperCase(),
                book.getAuthor(),
                book.getTitle(),
                book.getAuthor(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublishedDate(),
                book.getCategory(),
                book.getAuthor(),
                book.getTitle(),
                book.getTitle(),
                book.getTitle(),
                book.getTitle(),
                book.getTitle(),
                book.getCategory(),
                book.getTitle(),
                book.getTitle()
        );
    }

    /**
     * Gets reading time description
     */
    private String getReadingTimeDescription() {
        return "10-minute";
    }

    /**
     * Estimates reading time in minutes
     */
    private Integer getEstimatedReadingTime() {
        // Average reading speed is ~150-200 words per minute
        // A 10-minute essay would be approximately 1500-2000 words
        return 10;
    }

    /**
     * Asks DeepSeek AI to recommend top 3 music pieces for reading this book,
     * then generates Apple Music search URLs for those pieces.
     */
    private Essay.AppleMusicLink createAppleMusicLink(Book book) {
        List<String> recommendations = generateMusicRecommendations(book);

        StringBuilder formattedText = new StringBuilder();
        StringBuilder allUrls = new StringBuilder();
        String firstUrl = null;
        String firstDeepLink = null;

        for (int i = 0; i < recommendations.size(); i++) {
            String name = recommendations.get(i);
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String url = "https://music.apple.com/search?term=" + encoded;
            String deepLink = "music://music.apple.com/search?term=" + encoded;

            if (i == 0) {
                firstUrl = url;
                firstDeepLink = deepLink;
            }

            formattedText.append("🎵 ").append(i + 1).append(". ").append(name).append("\n");
            if (allUrls.length() > 0) allUrls.append("\n");
            allUrls.append(url);
        }

        formattedText.append("\nOpen in Apple Music to listen while reading.");

        return new Essay.AppleMusicLink(
                firstUrl != null ? firstUrl : "https://music.apple.com/search?term=classical+reading",
                firstDeepLink != null ? firstDeepLink : "music://music.apple.com/search?term=classical+reading",
                formattedText.toString()
        );
    }

    /**
     * Calls DeepSeek AI to get top 3 music recommendations for reading this book.
     */
    private List<String> generateMusicRecommendations(Book book) {
        String prompt = String.format("""
                You are a music expert. Recommend exactly 3 music pieces that would be \
                perfect background music while reading an essay about the book "%s" by %s \
                (category: %s).

                Choose music that matches the book's mood, era, themes, and atmosphere. \
                Prefer classical, ambient, jazz, or instrumental pieces suitable for \
                focused reading. Consider the book's historical period, cultural context, \
                and emotional tone.

                Output ONLY 3 lines in this exact format (nothing else):
                1. Piece Name - Composer/Artist
                2. Piece Name - Composer/Artist
                3. Piece Name - Composer/Artist
                """,
                book.getTitle(), book.getAuthor(), book.getCategory());

        try {
            String response = webClient.post()
                    .uri("https://api.deepseek.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of("role", "user", "content", prompt)
                            ),
                            "temperature", 0.7,
                            "max_tokens", 200
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                String content = extractContentFromResponse(response);
                if (content != null) {
                    return parseMusicRecommendations(content);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating music recommendations: " + e.getMessage());
        }

        return List.of(
                "Clair de Lune - Claude Debussy",
                "Gymnopedie No.1 - Erik Satie",
                "The Four Seasons - Antonio Vivaldi"
        );
    }

    /**
     * Parses the AI response to extract 3 music recommendation lines.
     */
    private List<String> parseMusicRecommendations(String content) {
        List<String> result = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            // Match lines starting with "1.", "2.", "3." or "1)", "2)", "3)"
            if (trimmed.matches("^[1-3][.)]\\s+.+")) {
                String name = trimmed.replaceFirst("^[1-3][.)]\\s+", "").trim();
                if (!name.isEmpty()) {
                    result.add(name);
                }
            }
        }
        return result.size() >= 3 ? result.subList(0, 3)
                : result.size() > 0 ? result
                : List.of("Clair de Lune - Claude Debussy",
                        "Gymnopedie No.1 - Erik Satie",
                        "The Four Seasons - Antonio Vivaldi");
    }

    /**
     * Lists available models from DeepSeek
     */
    public List<String> listModels() {
        return List.of(
                "deepseek-chat",
                "deepseek-coder"
        );
    }
}
