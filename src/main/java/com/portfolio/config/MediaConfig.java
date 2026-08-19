package com.portfolio.config;

import com.portfolio.media.MediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the Media module's externalized upload limits and storage location. */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaConfig {
}
