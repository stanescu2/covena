package io.covena.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "covena.storage.local")
public record StorageProperties(Path baseDir) {
}
