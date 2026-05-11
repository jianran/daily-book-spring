package com.dailybook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DailyBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyBookApplication.class, args);
    }
}
