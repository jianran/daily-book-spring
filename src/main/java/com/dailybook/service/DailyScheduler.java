package com.dailybook.service;

import com.dailybook.model.Essay;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyScheduler {

    private final BookService bookService;
    private final DiscordService discordService;

    public DailyScheduler(BookService bookService, DiscordService discordService) {
        this.bookService = bookService;
        this.discordService = discordService;
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    public void sendDailyEssay() {
        System.out.println("=== Daily essay job triggered at " + java.time.LocalDateTime.now() + " ===");
        try {
            Essay essay = bookService.generateDailyEssay();
            discordService.sendEssayToDirectMessage(essay);
            System.out.println("Daily essay sent: " + essay.getSelectedBook().getTitle()
                    + " by " + essay.getSelectedBook().getAuthor());
        } catch (Exception e) {
            System.err.println("Daily essay job failed: " + e.getMessage());
        }
    }
}
