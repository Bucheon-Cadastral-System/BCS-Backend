package com.is.bcs.adapter.out.file;

import com.is.bcs.application.dto.StoredControlPointImageFile;
import com.is.bcs.application.port.out.controlpointimage.ControlPointImageFileStoragePort;
import com.is.bcs.config.properties.ImageUploadProperties;
import com.is.bcs.domain.controlpointimage.exception.ControlPointImageStorageException;
import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LocalControlPointImageStorageAdapter implements ControlPointImageFileStoragePort {

    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private static final Duration WEBP_INFO_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Pattern CANVAS_SIZE_PATTERN =
            Pattern.compile("Canvas size\\s+(\\d+)\\s*x\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern WIDTH_PATTERN =
            Pattern.compile("Width:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEIGHT_PATTERN =
            Pattern.compile("Height:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern UNSAFE_ORIGINAL_FILE_NAME =
            Pattern.compile("[\\p{Cntrl}]");

    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;

    private final ImageUploadProperties properties;
    private final ControlPointImageFileNameGenerator fileNameGenerator;

    @Override
    public StoredControlPointImageFile store(
            Long projectId,
            Long pointId,
            String pointName,
            String originalFileName,
            String contentType,
            long declaredFileSize,
            byte[] content
    ) {
        validateBasicFile(contentType, declaredFileSize, content);

        String safeOriginalFileName = sanitizeOriginalFileName(originalFileName);

        String storedFileName = fileNameGenerator.generate(pointName);

        Path relativePath = Path.of(
                "control-points",
                pointId.toString(),
                "projects",
                projectId.toString(),
                storedFileName
        );

        Path rootPath = properties.rootPath();
        Path targetPath = resolveSafely(rootPath, relativePath);
        Path directory = targetPath.getParent();

        Path temporaryPath = null;

        try {
            Files.createDirectories(directory);

            temporaryPath = directory.resolve(".upload-" + UUID.randomUUID() + ".tmp");

            Files.write(
                    temporaryPath,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            ImageDimensions dimensions = validateWithWebpInfo(temporaryPath);

            validateDimensions(dimensions);

            moveToFinalPath(
                    temporaryPath,
                    targetPath
            );

            return new StoredControlPointImageFile(
                    storedFileName,
                    toStoragePath(relativePath),
                    safeOriginalFileName,
                    WEBP_CONTENT_TYPE,
                    content.length,
                    dimensions.width(),
                    dimensions.height()
            );
        } catch (InvalidControlPointImageException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw exception;
        } catch (IOException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw new ControlPointImageStorageException("이미지 파일을 저장할 수 없습니다.", exception);
        } catch (RuntimeException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw exception;
        }
    }

    @Override
    public void deleteIfExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path rootPath = properties.rootPath();
        Path relativePath;

        try {
            relativePath = Path.of(storagePath);
        } catch (InvalidPathException exception) {
            throw new ControlPointImageStorageException("저장된 이미지 경로가 올바르지 않습니다.", exception);
        }

        Path targetPath = resolveSafely(rootPath, relativePath);

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new ControlPointImageStorageException("이미지 파일을 삭제할 수 없습니다.", exception);
        }
    }

    private void validateBasicFile(String contentType, long declaredFileSize, byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidControlPointImageException("빈 이미지 파일은 등록할 수 없습니다.");
        }

        long maxFileSize =
                properties.maxFileSize().toBytes();

        if (content.length > maxFileSize) {
            throw new InvalidControlPointImageException("이미지는 한 장당 최대 %s까지 등록할 수 있습니다.".formatted(properties.maxFileSize()));
        }

        if (declaredFileSize != content.length) {
            throw new InvalidControlPointImageException("전달된 이미지 파일 크기가 일치하지 않습니다.");
        }

        if (contentType == null || !WEBP_CONTENT_TYPE.equals(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidControlPointImageException("WebP 이미지만 등록할 수 있습니다.");
        }

        validateWebpSignature(content);
    }

    /**
     * WebP는 RIFF 컨테이너이며 처음 12바이트가 다음 형식이어야 한다.
     *
     * 0~3: RIFF
     * 4~7: 파일 크기 정보
     * 8~11: WEBP
     */
    private static void validateWebpSignature(byte[] content) {
        if (content.length < 12
                || content[0] != 'R'
                || content[1] != 'I'
                || content[2] != 'F'
                || content[3] != 'F'
                || content[8] != 'W'
                || content[9] != 'E'
                || content[10] != 'B'
                || content[11] != 'P') {
            throw new InvalidControlPointImageException("올바른 WebP 파일이 아닙니다.");
        }
    }

    private ImageDimensions validateWithWebpInfo(Path temporaryPath) {
        Process process;

        try {
            process = new ProcessBuilder(properties.webpInfoCommand(), temporaryPath.toString())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            throw new ControlPointImageStorageException("WebP 검사 도구를 실행할 수 없습니다.", exception);
        }

        boolean finished;

        try {
            finished = process.waitFor(WEBP_INFO_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();

            throw new ControlPointImageStorageException("WebP 파일 검사가 중단되었습니다.", exception);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new ControlPointImageStorageException("WebP 파일 검사 시간이 초과되었습니다.");
        }

        String output;

        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ControlPointImageStorageException("WebP 검사 결과를 읽을 수 없습니다.", exception);
        }

        if (process.exitValue() != 0) {
            throw new InvalidControlPointImageException("손상되었거나 올바르지 않은 WebP 파일입니다.");
        }

        return parseDimensions(output);
    }

    private static ImageDimensions parseDimensions(String output) {
        Matcher canvasMatcher = CANVAS_SIZE_PATTERN.matcher(output);

        if (canvasMatcher.find()) {
            return new ImageDimensions(parseDimension(canvasMatcher.group(1)), parseDimension(canvasMatcher.group(2)));
        }

        Matcher widthMatcher = WIDTH_PATTERN.matcher(output);

        Matcher heightMatcher = HEIGHT_PATTERN.matcher(output);

        if (widthMatcher.find() && heightMatcher.find()) {
            return new ImageDimensions(
                    parseDimension(widthMatcher.group(1)),
                    parseDimension(heightMatcher.group(1))
            );
        }

        throw new InvalidControlPointImageException("WebP 이미지 크기를 확인할 수 없습니다.");
    }

    private static int parseDimension(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new InvalidControlPointImageException("WebP 이미지 크기가 올바르지 않습니다.");
        }
    }

    private void validateDimensions(ImageDimensions dimensions) {
        if (dimensions.width() <= 0 || dimensions.height() <= 0) {
            throw new InvalidControlPointImageException("WebP 이미지 크기가 올바르지 않습니다.");
        }

        if (dimensions.width() > properties.maxWidth() || dimensions.height() > properties.maxHeight()) {
            throw new InvalidControlPointImageException("이미지 크기는 최대 %d × %d 픽셀까지 등록할 수 있습니다."
                    .formatted(properties.maxWidth(), properties.maxHeight()));
        }
    }

    private static void moveToFinalPath(Path temporaryPath, Path targetPath) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            /*
             * 같은 디렉터리 안의 이동이지만 파일시스템이 원자적 이동을
             * 지원하지 않으면 일반 이동으로 처리한다.
             * REPLACE_EXISTING을 사용하지 않아 기존 파일을 덮어쓰지 않는다.
             */
            Files.move(temporaryPath, targetPath);
        }
    }

    private static Path resolveSafely(Path rootPath, Path relativePath) {
        if (relativePath.isAbsolute()) {
            throw new ControlPointImageStorageException("이미지 저장 경로는 상대 경로여야 합니다.");
        }

        Path normalizedRoot = rootPath.toAbsolutePath().normalize();

        Path resolved = normalizedRoot
                .resolve(relativePath)
                .normalize();

        if (!resolved.startsWith(normalizedRoot)) {
            throw new ControlPointImageStorageException("허용된 이미지 저장 경로를 벗어날 수 없습니다.");
        }

        return resolved;
    }

    private static String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "image.webp";
        }

        String normalized = Normalizer.normalize(
                originalFileName.trim(),
                Normalizer.Form.NFC
        );

        /*
         * 브라우저 또는 운영체제가 전체 경로를 보내더라도
         * 마지막 파일명 부분만 메타데이터로 보관한다.
         */
        normalized = normalized.replace('\\', '/');

        int lastSeparator = normalized.lastIndexOf('/');

        if (lastSeparator >= 0) {
            normalized = normalized.substring(lastSeparator + 1);
        }

        normalized = UNSAFE_ORIGINAL_FILE_NAME
                .matcher(normalized)
                .replaceAll("_");

        if (normalized.isBlank()) {
            return "image.webp";
        }

        return truncate(normalized, MAX_ORIGINAL_FILE_NAME_LENGTH);
    }

    private static String truncate(String value, int maxCodePoints
) {
        int codePointCount = value.codePointCount(0, value.length());

        if (codePointCount <= maxCodePoints) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxCodePoints);

        return value.substring(0, endIndex);
    }

    /**
     * 운영체제별 경로 구분자를 DB에 저장하지 않도록
     * 상대 경로는 항상 '/' 형식으로 저장한다.
     */
    private static String toStoragePath(Path relativePath) {
        return relativePath.toString()
                .replace('\\', '/');
    }

    private static void deleteTemporaryFileQuietly(Path temporaryPath) {
        if (temporaryPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryPath);
        } catch (IOException ignored) {
            // 원래 업로드 실패 예외를 유지한다.
        }
    }

    private record ImageDimensions(int width, int height
    ) {
    }
}