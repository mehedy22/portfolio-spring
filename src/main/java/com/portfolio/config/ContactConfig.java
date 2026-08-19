package com.portfolio.config;

import com.portfolio.contact.ContactProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the Contact module's externalized spam-protection settings. */
@Configuration
@EnableConfigurationProperties(ContactProperties.class)
public class ContactConfig {
}
