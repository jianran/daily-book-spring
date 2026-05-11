package com.dailybook.service;

import com.dailybook.model.Essay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiscordService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final String userId;
    private final String authToken;

    private static final int MAX_DESC_LENGTH = 3500; // Leave room for title, fields, footer (6000 total limit)
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DiscordService(
            @Value("${discord.webhook.url:}") String webhookUrl,
            @Value("${discord.user.id:}") String userId,
            @Value("${discord.auth.token:}") String authToken) {
        this.webClient = WebClient.create();
        this.objectMapper = new ObjectMapper();
        this.webhookUrl = webhookUrl;
        this.userId = userId;
        this.authToken = authToken;
    }

    public void sendEssayToDiscord(Essay essay) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("your")) {
            System.err.println("Discord webhook URL not configured");
            return;
        }
        doSend(webhookUrl, null, essay);
    }

    public void sendEssayToDirectMessage(Essay essay) {
        if (userId == null || userId.isEmpty() || authToken == null || authToken.isEmpty() ||
            authToken.contains("your") || authToken.contains("placeholder")) {
            System.err.println("Discord DM credentials not configured");
            return;
        }

        try {
            String dmChannelId = getOrCreateDMChannel();
            if (dmChannelId == null) {
                System.err.println("Failed to get DM channel");
                return;
            }
            doSend("https://discord.com/api/v10/channels/" + dmChannelId + "/messages", authToken, essay);
        } catch (Exception e) {
            System.err.println("Error sending to Discord DM: " + e.getMessage());
        }
    }

    private void doSend(String url, String authToken, Essay essay) {
        try {
            Map<String, Object> payload = buildPayload(essay);

            var request = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload);

            if (authToken != null) {
                request.header("Authorization", "Bot " + authToken);
            }

            String response = request.retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Essay sent to Discord successfully");
        } catch (WebClientResponseException e) {
            System.err.println("Discord API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Error sending to Discord: " + e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(Essay essay) {
        String bookTitle = essay.getSelectedBook().getTitle();
        String author = essay.getSelectedBook().getAuthor();
        String category = essay.getSelectedBook().getCategory();
        String essayContent = essay.getContent();
        String musicLink = essay.getAppleMusicLink().getFormattedText();
        String date = essay.getGeneratedAt().format(DATE_FMT);
        String timestamp = essay.getGeneratedAt().toString();

        // Split essay into chunks that fit Discord embed description (max 4096)
        List<String> chunks = splitContent(essayContent, MAX_DESC_LENGTH);

        List<Map<String, Object>> embeds = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> embed = new LinkedHashMap<>();
            embed.put("color", 3447003);
            embed.put("description", chunks.get(i));
            embed.put("timestamp", timestamp);

            if (i == 0) {
                embed.put("title", bookTitle);
                List<Map<String, Object>> fields = new ArrayList<>();
                fields.add(field("📚 Author", author, true));
                fields.add(field("📂 Category", category, true));
                fields.add(field("🎵 Background Music", musicLink, false));
                embed.put("fields", fields);
            }

            String partLabel = chunks.size() > 1 ? " • Part " + (i + 1) + "/" + chunks.size() : "";
            Map<String, String> footer = new LinkedHashMap<>();
            footer.put("text", "Daily Book • " + date + partLabel);
            embed.put("footer", footer);

            embeds.add(embed);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("embeds", embeds);

        return payload;
    }

    private static Map<String, Object> field(String name, String value, boolean inline) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    static List<String> splitContent(String content, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxLen, content.length());
            // Try to break at a paragraph boundary
            if (end < content.length()) {
                int breakPoint = content.lastIndexOf("\n\n", end);
                if (breakPoint > start && breakPoint > end - 500) {
                    end = breakPoint + 2;
                } else {
                    breakPoint = content.lastIndexOf("\n", end);
                    if (breakPoint > start && breakPoint > end - 200) {
                        end = breakPoint + 1;
                    } else {
                        // Break at last space
                        breakPoint = content.lastIndexOf(" ", end);
                        if (breakPoint > start && breakPoint > end - 100) {
                            end = breakPoint + 1;
                        }
                    }
                }
            }
            chunks.add(content.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private String getOrCreateDMChannel() {
        try {
            String response = webClient.get()
                    .uri("https://discord.com/api/v10/users/@me/channels")
                    .header("Authorization", "Bot " + authToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                int dmIndex = response.indexOf("\"type\":6");
                if (dmIndex != -1) {
                    int idStart = response.substring(0, dmIndex).lastIndexOf("\"id\":");
                    if (idStart != -1) {
                        int quoteStart = response.indexOf('"', idStart + 5);
                        int quoteEnd = response.indexOf('"', quoteStart + 1);
                        if (quoteEnd != -1) {
                            return response.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            }
            return createNewDMChannel();
        } catch (Exception e) {
            System.err.println("Error getting DM channel: " + e.getMessage());
            return createNewDMChannel();
        }
    }

    private String createNewDMChannel() {
        try {
            String payload = String.format("{\"recipient_id\": \"%s\"}", userId);
            String response = webClient.post()
                    .uri("https://discord.com/api/v10/users/@me/channels")
                    .header("Authorization", "Bot " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                int idStart = response.indexOf("\"id\":");
                if (idStart != -1) {
                    int quoteStart = response.indexOf('"', idStart + 5);
                    int quoteEnd = response.indexOf('"', quoteStart + 1);
                    if (quoteEnd != -1) {
                        return response.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error creating DM channel: " + e.getMessage());
            return null;
        }
    }
}
