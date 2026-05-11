package com.dailybook.service;

import com.dailybook.model.Essay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DiscordService {

    private final WebClient webClient;
    private final String webhookUrl;
    private final String userId;
    private final String authToken;

    public DiscordService(
            @Value("${discord.webhook.url:}") String webhookUrl,
            @Value("${discord.user.id:}") String userId,
            @Value("${discord.auth.token:}") String authToken) {
        this.webClient = WebClient.create();
        this.webhookUrl = webhookUrl;
        this.userId = userId;
        this.authToken = authToken;
    }

    /**
     * Sends essay and music link to Discord via webhook
     */
    public void sendEssayToDiscord(Essay essay) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("your")) {
            System.err.println("Discord webhook URL not configured");
            return;
        }

        try {
            String embedsJson = buildEmbedsJson(essay);
            String payload = String.format("{\"content\": null, \"embeds\": [%s]}", embedsJson);

            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            System.out.println("Essay sent to Discord successfully");
        } catch (Exception e) {
            System.err.println("Error sending to Discord: " + e.getMessage());
        }
    }

    /**
     * Sends essay directly to user's DM channel
     */
    public void sendEssayToDirectMessage(Essay essay) {
        if (userId == null || userId.isEmpty() || authToken == null || authToken.isEmpty() ||
            authToken.contains("your") || authToken.contains("placeholder")) {
            System.err.println("Discord DM credentials not configured");
            return;
        }

        try {
            // First, get the user's DM channel ID
            String dmChannelId = getOrCreateDMChannel();
            if (dmChannelId == null) {
                System.err.println("Failed to get DM channel");
                return;
            }

            // Send essay in multiple embeds (Discord limits: 4096 chars per embed description)
            String embedsJson = buildEmbedsJson(essay);
            String payload = String.format("{\"content\": null, \"embeds\": [%s]}", embedsJson);

            webClient.post()
                    .uri("https://discord.com/api/v10/channels/" + dmChannelId + "/messages")
                    .header("Authorization", "Bot " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            System.out.println("Essay sent to Discord DM successfully");
        } catch (Exception e) {
            System.err.println("Error sending to Discord DM: " + e.getMessage());
        }
    }

    /**
     * Gets or creates the user's DM channel
     */
    private String getOrCreateDMChannel() {
        try {
            String response = webClient.get()
                    .uri("https://discord.com/api/v10/users/@me/channels")
                    .header("Authorization", "Bot " + authToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                // Parse JSON to find DM channel
                int dmIndex = response.indexOf("\"type\":6");
                if (dmIndex != -1) {
                    // Find the channel ID
                    int idStart = response.substring(0, dmIndex).lastIndexOf("\"id\":");
                    if (idStart != -1) {
                        int quoteStart = response.indexOf('"', idStart + 5);
                        int quoteEnd = response.indexOf('"', quoteStart + 1);
                        if (quoteEnd != -1) {
                            String channelId = response.substring(quoteStart + 1, quoteEnd);
                            return channelId;
                        }
                    }
                }
            }

            // If no existing DM found, create new one
            return createNewDMChannel();
        } catch (Exception e) {
            System.err.println("Error getting DM channel: " + e.getMessage());
            return createNewDMChannel();
        }
    }

    /**
     * Creates a new DM channel with the user
     */
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

  /**
     * Builds Discord embed JSON for the essay
     */
    private String buildEmbedsJson(Essay essay) {
        String bookTitle = escapeJson(essay.getSelectedBook().getTitle());
        String author = escapeJson(essay.getSelectedBook().getAuthor());
        String category = escapeJson(essay.getSelectedBook().getCategory());
        String musicLink = escapeJson(essay.getAppleMusicLink().getFormattedText());
        String formattedDate = essay.getGeneratedAt().toString().replace('T', ' ');
        String timestamp = essay.getGeneratedAt().toString();
        String essayContent = escapeJson(essay.getContent());

        // Split essay for Discord embeds (description max 4096, field value max 1024)
        // Use description for essay content, fields only for metadata
        int maxDesc = 4000;
        int totalLen = essayContent.length();
        String part1 = essayContent.substring(0, Math.min(maxDesc, totalLen));
        String part2 = totalLen > maxDesc ? essayContent.substring(maxDesc, Math.min(maxDesc * 2, totalLen)) : "";

        StringBuilder embeds = new StringBuilder();

        // Embed 1: essay part 1 + book info
        embeds.append("{\n");
        embeds.append("  \"title\": \"").append(bookTitle).append("\",\n");
        embeds.append("  \"description\": \"").append(part1).append("\",\n");
        embeds.append("  \"color\": 3447003,\n");
        embeds.append("  \"fields\": [\n");
        embeds.append("    {\n");
        embeds.append("      \"name\": \"📚 Author\",\n");
        embeds.append("      \"value\": \"").append(author).append("\",\n");
        embeds.append("      \"inline\": true\n");
        embeds.append("    },\n");
        embeds.append("    {\n");
        embeds.append("      \"name\": \"📂 Category\",\n");
        embeds.append("      \"value\": \"").append(category).append("\",\n");
        embeds.append("      \"inline\": true\n");
        embeds.append("    },\n");
        embeds.append("    {\n");
        embeds.append("      \"name\": \"🎵 Background Music\",\n");
        embeds.append("      \"value\": \"").append(musicLink).append("\",\n");
        embeds.append("      \"inline\": false\n");
        embeds.append("    }\n");
        embeds.append("  ],\n");
        embeds.append("  \"footer\": {\n");
        embeds.append("    \"text\": \"Daily Book • ").append(formattedDate).append(" • Part 1").append(part2.isEmpty() ? "" : "/2").append("\"\n");
        embeds.append("  },\n");
        embeds.append("  \"timestamp\": \"").append(timestamp).append("\"\n");
        embeds.append("}");

        if (!part2.isEmpty()) {
            embeds.append(",\n");
            embeds.append("{\n");
            embeds.append("  \"description\": \"").append(part2).append("\",\n");
            embeds.append("  \"color\": 3447003,\n");
            embeds.append("  \"fields\": [\n");
            embeds.append("    {\n");
            embeds.append("      \"name\": \"🎵 Background Music\",\n");
            embeds.append("      \"value\": \"").append(musicLink).append("\",\n");
            embeds.append("      \"inline\": false\n");
            embeds.append("    }\n");
            embeds.append("  ],\n");
            embeds.append("  \"footer\": {\n");
            embeds.append("    \"text\": \"Daily Book • ").append(formattedDate).append(" • Part 2/2\"\n");
            embeds.append("  },\n");
            embeds.append("  \"timestamp\": \"").append(timestamp).append("\"\n");
            embeds.append("}");
        }

        return embeds.toString();
    }

    /**
     * Escapes special characters for JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
