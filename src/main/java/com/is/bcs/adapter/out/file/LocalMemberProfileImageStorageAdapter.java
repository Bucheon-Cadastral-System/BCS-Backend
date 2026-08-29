package com.is.bcs.adapter.out.file;

import com.is.bcs.application.port.out.member.MemberProfileImageStoragePort;
import com.is.bcs.config.properties.ImageUploadProperties;
import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import com.is.bcs.domain.member.exception.InvalidMemberProfileImageException;
import com.is.bcs.domain.member.exception.MemberProfileImageStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalMemberProfileImageStorageAdapter implements MemberProfileImageStoragePort {

    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private final ImageUploadProperties properties;

    @Override
    public String store(
            Long memberId,
            String contentType,
            long declaredFileSize,
            byte[] content
    ) {
        validateBasicFile(contentType, declaredFileSize, content);

        WebpHeader.Dimensions dimensions = readDimensions(content);

        validateDimensions(dimensions);

        String storedFileName = UUID.randomUUID() + ".webp";

        Path relativePath = Path.of(
                "profile-images",
                memberId.toString(),
                storedFileName
        );

        Path targetPath = resolveSafely(properties.rootPath(), relativePath);

        Path directory = targetPath.getParent();
        Path temporaryPath = null;

        try {
            Files.createDirectories(directory);

            temporaryPath = Files.createTempFile(
                    directory,
                    ".upload-",
                    ".tmp"
            );

            Files.write(
                    temporaryPath,
                    content,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            moveToFinalPath(
                    temporaryPath,
                    targetPath
            );

            return toStoragePath(relativePath);

        } catch (IOException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw new MemberProfileImageStorageException("프로필 이미지 파일을 저장할 수 없습니다.", exception);

        } catch (RuntimeException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw exception;
        }

    }

    @Override
    public byte[] read(String storagePath) {
        Path targetPath = resolveStoragePath(storagePath);

        if (!Files.isRegularFile(targetPath)) {
            throw new MemberProfileImageStorageException(
                    "저장된 프로필 이미지 파일을 찾을 수 없습니다."
            );
        }

        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException exception) {
            throw new MemberProfileImageStorageException(
                    "프로필 이미지 파일을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    @Override
    public void deleteIfExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path targetPath = resolveStoragePath(storagePath);

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new MemberProfileImageStorageException(
                    "프로필 이미지 파일을 삭제할 수 없습니다.",
                    exception
            );
        }
    }

    private void validateBasicFile(
            String contentType,
            long declaredFileSize,
            byte[] content
    ) {
        if (content == null || content.length == 0) {
            throw new InvalidMemberProfileImageException(
                    "빈 프로필 이미지는 등록할 수 없습니다."
            );
        }

        if (content.length > properties.maxFileSize().toBytes()) {
            throw new InvalidMemberProfileImageException(
                    "프로필 이미지는 최대 %s까지 등록할 수 있습니다."
                            .formatted(properties.maxFileSize())
            );
        }

        if (declaredFileSize != content.length) {
            throw new InvalidMemberProfileImageException(
                    "전달된 프로필 이미지 파일 크기가 일치하지 않습니다."
            );
        }

        if (contentType == null
                || !WEBP_CONTENT_TYPE.equals(
                        contentType.toLowerCase(Locale.ROOT)
                )) {
            throw new InvalidMemberProfileImageException(
                    "WebP 이미지만 등록할 수 있습니다."
            );
        }
    }

    private static WebpHeader.Dimensions readDimensions(
            byte[] content
    ) {
        try {
            return WebpHeader.read(content);
        } catch (InvalidControlPointImageException exception) {
            throw new InvalidMemberProfileImageException(
                    exception.getMessage()
            );
        }
    }

    private void validateDimensions(
            WebpHeader.Dimensions dimensions
    ) {
        if (dimensions.width() <= 0
                || dimensions.height() <= 0) {
            throw new InvalidMemberProfileImageException(
                    "프로필 이미지 크기가 올바르지 않습니다."
            );
        }

        if (dimensions.width() > properties.maxWidth()
                || dimensions.height() > properties.maxHeight()) {
            throw new InvalidMemberProfileImageException(
                    "프로필 이미지 크기는 최대 %d × %d 픽셀까지 등록할 수 있습니다."
                            .formatted(
                                    properties.maxWidth(),
                                    properties.maxHeight()
                            )
            );
        }
    }

    private Path resolveStoragePath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new MemberProfileImageStorageException(
                    "저장된 프로필 이미지 경로가 없습니다."
            );
        }

        Path relativePath;

        try {
            relativePath = Path.of(storagePath);
        } catch (InvalidPathException exception) {
            throw new MemberProfileImageStorageException(
                    "저장된 프로필 이미지 경로가 올바르지 않습니다.",
                    exception
            );
        }

        return resolveSafely(
                properties.rootPath(),
                relativePath
        );
    }

    private static Path resolveSafely(
            Path rootPath,
            Path relativePath
    ) {
        if (relativePath.isAbsolute()) {
            throw new MemberProfileImageStorageException(
                    "프로필 이미지 저장 경로는 상대 경로여야 합니다."
            );
        }

        Path normalizedRoot =
                rootPath.toAbsolutePath().normalize();

        Path resolved =
                normalizedRoot
                        .resolve(relativePath)
                        .normalize();

        if (!resolved.startsWith(normalizedRoot)) {
            throw new MemberProfileImageStorageException(
                    "허용된 프로필 이미지 경로를 벗어날 수 없습니다."
            );
        }

        return resolved;
    }

    private static void moveToFinalPath(Path temporaryPath, Path targetPath) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, targetPath);
        }
    }

    private static String toStoragePath(Path relativePath) {
        return relativePath
                .toString()
                .replace('\\', '/');
    }

    private static void deleteTemporaryFileQuietly(Path temporaryPath) {
        if (temporaryPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryPath);
        } catch (IOException ignored) {
            // 기존 업로드 실패 예외를 유지한다.
        }
    }
}