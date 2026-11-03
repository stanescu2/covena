package io.covena.document.adapter.out.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxCleanupProperties.class)
class OutboxCleanupConfiguration {
}
