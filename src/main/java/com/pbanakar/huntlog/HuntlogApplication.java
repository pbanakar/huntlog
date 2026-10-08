package com.pbanakar.huntlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for HuntLog.
 * 
 * Note on @EnableScheduling:
 * @EnableScheduling is declared on the main application configuration class rather than
 * on individual service classes. This registers the background task executor globally across
 * the entire Spring ApplicationContext, separating infrastructure configuration from business
 * logic and allowing service classes (like ReminderService) to be unit-tested without
 * triggering background scheduler threads.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class HuntlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuntlogApplication.class, args);
    }
}
