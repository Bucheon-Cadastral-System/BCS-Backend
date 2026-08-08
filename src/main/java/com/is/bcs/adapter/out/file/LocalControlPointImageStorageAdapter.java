package com.is.bcs.adapter.out.file;

import com.is.bcs.application.dto.StoredControlPointImageFile;
import com.is.bcs.application.port.out.controlpointimage.ControlPointImageFileStoragePort;
import com.is.bcs.config.properties.ImageUploadProperties;
import com.is.bcs.domain.controlpointimage.exception.ControlPointImageStorageException;
import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LocalControlPointImageStorageAdapter implements ControlPointImageFileStoragePort {

    private static final String WEBP_CONTENT_TYPE = "image/webp";

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
            OffsetDateTime capturedAt,
            String originalFileName,
            String contentType,
            long declaredFileSize,
            byte[] content
    ) {
        validateBasicFile(contentType, declaredFileSize, content);

        /*
         * 크기 확인을 디스크보다 먼저 한다. 종전에는 임시 파일을 만든 뒤 그 파일을 외부 명령에 물려
         * 크기를 읽었으므로, 너무 큰 사진도 일단 디스크에 한 번 쓰이고 나서 거절됐다.
         */
        ImageDimensions dimensions = readDimensions(content);

        validateDimensions(dimensions);

        String safeOriginalFileName = sanitizeOriginalFileName(originalFileName);

        String storedFileName = fileNameGenerator.generate(pointName, capturedAt);

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

    @Override
    public byte[] read(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new ControlPointImageStorageException("저장된 이미지 경로가 없습니다.");
        }

        Path relativePath;

        try {
            relativePath = Path.of(storagePath);
        } catch (InvalidPathException exception) {
            throw new ControlPointImageStorageException("저장된 이미지 경로가 올바르지 않습니다.", exception);
        }

        Path targetPath = resolveSafely(properties.rootPath(), relativePath);

        if (!Files.isRegularFile(targetPath)) {
            throw new ControlPointImageStorageException("저장된 이미지 파일을 찾을 수 없습니다.");
        }

        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException exception) {
            throw new ControlPointImageStorageException("이미지 파일을 읽을 수 없습니다.", exception);
        }
    }

    /**
     * 파일 자체의 조건만 본다 — 비어 있지 않은지, 한도 안인지, 선언한 크기와 맞는지, Content-Type 이 WebP 인지.
     *
     * <p>내용이 정말 WebP 인지는 여기서 보지 않는다. {@link WebpHeader} 가 크기를 읽으면서 함께 판정하므로
     * 형식 검사를 두 벌로 두지 않는다.
     */
    private void validateBasicFile(String contentType, long declaredFileSize, byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidControlPointImageException("빈 이미지 파일은 등록할 수 없습니다.");
        }

        long maxFileSize = properties.maxFileSize().toBytes();

        if (content.length > maxFileSize) {
            throw new InvalidControlPointImageException("이미지는 한 장당 최대 %s까지 등록할 수 있습니다.".formatted(properties.maxFileSize()));
        }

        if (declaredFileSize != content.length) {
            throw new InvalidControlPointImageException("전달된 이미지 파일 크기가 일치하지 않습니다.");
        }

        if (contentType == null || !WEBP_CONTENT_TYPE.equals(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidControlPointImageException("WebP 이미지만 등록할 수 있습니다.");
        }
    }

    private static ImageDimensions readDimensions(byte[] content) {
        WebpHeader.Dimensions dimensions = WebpHeader.read(content);
        return new ImageDimensions(dimensions.width(), dimensions.height());
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