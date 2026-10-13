package io.covena.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ServicesConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
