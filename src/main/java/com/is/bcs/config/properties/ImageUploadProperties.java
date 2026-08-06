package com.is.bcs.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.image-upload")
public record ImageUploadProperties(
        String rootDirectory,
        DataSize maxFileSize,
        int maxWidth,
        int maxHeight,
        String webpInfoCommand
) {

    public ImageUploadProperties {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            throw new IllegalArgumentException("이미지 업로드 루트 경로는 필수입니다.");
        }

        Objects.requireNonNull(
                maxFileSize,
                "이미지 최대 파일 크기는 필수입니다."
        );

        if (maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException("이미지 최대 파일 크기는 0보다 커야 합니다.");
        }

        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("이미지 최대 가로·세로 크기는 0보다 커야 합니다.");
        }

        if (webpInfoCommand == null || webpInfoCommand.isBlank()) {
            throw new IllegalArgumentException("webpinfo 명령어는 필수입니다.");
        }
    }

    public Path rootPath() {
        return Path.of(rootDirectory)
                .toAbsolutePath()
                .normalize();
    }
}