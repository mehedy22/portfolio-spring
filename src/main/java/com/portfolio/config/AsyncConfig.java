package com.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} for the notification listeners (D-007 keeps this in-process — there is no
 * broker, and a personal site does not need one).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
