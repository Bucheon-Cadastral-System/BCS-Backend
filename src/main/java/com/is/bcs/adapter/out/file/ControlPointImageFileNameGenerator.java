package com.is.bcs.adapter.out.file;

import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class ControlPointImageFileNameGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern REPEATED_UNDERSCORES = Pattern.compile("_+");

    private static final int MAX_POINT_NAME_LENGTH = 50;

    private static final ZoneId FILE_NAME_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 서버가 기준점명과 현재 날짜를 사용해 안전한 WebP 저장 파일명을 만든다.
     *
     * 예:
     * 2026_08_05_1465공_550e8400-e29b-41d4-a716-446655440000.webp
     */
    public String generate(String pointName, OffsetDateTime capturedAt) {
        String safePointName = sanitizePointName(pointName);
        String date = Objects.requireNonNull(capturedAt, "사진 촬영시각은 필수입니다.")
                .atZoneSameInstant(FILE_NAME_ZONE)
                .toLocalDate()
                .format(DATE_FORMAT);

        return "%s_%s_%s.webp".formatted(
                date,
                safePointName,
                UUID.randomUUID()
        );
    }

    private static String sanitizePointName(String pointName) {
        if (pointName == null || pointName.isBlank()) {
            throw new InvalidControlPointImageException("기준점명은 필수입니다.");
        }

        // macOS 자모 분리형 문자열도 서버에서 동일한 파일명 형태가 되도록 NFC로 정규화한다.
        String normalized = Normalizer.normalize(
                pointName.trim(),
                Normalizer.Form.NFC
        );

        String sanitized = UNSAFE_CHARACTERS.matcher(normalized).replaceAll("_");

        sanitized = WHITESPACE.matcher(sanitized).replaceAll("_");

        sanitized = REPEATED_UNDERSCORES.matcher(sanitized).replaceAll("_");

        sanitized = stripEdgeCharacters(sanitized);

        if (sanitized.isBlank()) {
            throw new InvalidControlPointImageException("파일명으로 사용할 수 없는 기준점명입니다.");
        }

        return truncate(sanitized, MAX_POINT_NAME_LENGTH);
    }

    private static String stripEdgeCharacters(String value) {
        int start = 0;
        int end = value.length();

        while (start < end && isEdgeCharacter(value.charAt(start))) {
            start++;
        }

        while (end > start && isEdgeCharacter(value.charAt(end - 1))) {
            end--;
        }

        return value.substring(start, end);
    }

    private static boolean isEdgeCharacter(char value) {
        return value == '.' || value == '_' || Character.isWhitespace(value);
    }

    private static String truncate(String value, int maxCodePoints) {
        int codePointCount = value.codePointCount(0, value.length());

        if (codePointCount <= maxCodePoints) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex);
    }
}