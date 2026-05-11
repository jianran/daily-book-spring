package com.dailybook.service;

import com.dailybook.model.Essay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiscordService {

    private final WebClient webClient;
    private final String webhookUrl;
    private final String userId;
    private final String authToken;

    private static final int PREVIEW_LENGTH = 500;
    private static final int MSG_CHUNK_SIZE = 1900;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DiscordService(
            @Value("${discord.webhook.url:}") String webhookUrl,
            @Value("${discord.user.id:}") String userId,
            @Value("${discord.auth.token:}") String authToken) {
        this.webClient = WebClient.create();
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
            // Step 1: Send book info embed with essay preview
            sendMessage(url, authToken, buildEmbedPayload(essay));

            // Step 2: Send full essay as plain text messages (2000 char Discord limit)
            List<String> chunks = splitContent(essay.getContent(), MSG_CHUNK_SIZE);
            for (int i = 0; i < chunks.size(); i++) {
                String label = chunks.size() > 1 ? "**" + essay.getSelectedBook().getTitle()
                        + "** (part " + (i + 1) + "/" + chunks.size() + ")\n\n" : "";
                Map<String, Object> textPayload = new LinkedHashMap<>();
                textPayload.put("content", label + chunks.get(i));
                sendMessage(url, authToken, textPayload);
            }

            System.out.println("Essay sent to Discord successfully");
        } catch (WebClientResponseException e) {
            System.err.println("Discord API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Error sending to Discord: " + e.getMessage());
        }
    }

    private void sendMessage(String url, String authToken, Map<String, Object> payload) {
        var request = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload);

        if (authToken != null) {
            request.header("Authorization", "Bot " + authToken);
        }

        request.retrieve().toBodilessEntity().block();
    }

    private Map<String, Object> buildEmbedPayload(Essay essay) {
        String bookTitle = essay.getSelectedBook().getTitle();
        String author = essay.getSelectedBook().getAuthor();
        String category = essay.getSelectedBook().getCategory();
        String musicLink = essay.getAppleMusicLink().getFormattedText();
        String date = essay.getGeneratedAt().format(DATE_FMT);
        String timestamp = essay.getGeneratedAt().toString();

        // Preview: first ~500 chars of the essay
        String preview = essay.getContent();
        if (preview.length() > PREVIEW_LENGTH) {
            preview = preview.substring(0, PREVIEW_LENGTH) + "...";
        }

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("📚 Author", author, true));
        fields.add(field("📂 Category", category, true));
        fields.add(field("📖 Preview", preview, false));
        fields.add(field("🎵 Background Music", musicLink, false));

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", bookTitle);
        embed.put("color", 3447003);
        embed.put("fields", fields);
        embed.put("timestamp", timestamp);

        Map<String, String> footer = new LinkedHashMap<>();
        footer.put("text", "Daily Book • " + date + " • Full essay follows below");
        embed.put("footer", footer);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("embeds", List.of(embed));
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
            if (end < content.length()) {
                int bp = content.lastIndexOf("\n\n", end);
                if (bp > start && bp > end - 400) {
                    end = bp + 2;
                } else {
                    bp = content.lastIndexOf("\n", end);
                    if (bp > start && bp > end - 150) {
                        end = bp + 1;
                    } else {
                        bp = content.lastIndexOf(" ", end);
                        if (bp > start && bp > end - 80) {
                            end = bp + 1;
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
