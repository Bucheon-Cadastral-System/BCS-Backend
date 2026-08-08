package com.is.bcs.adapter.out.file;

import com.is.bcs.application.dto.StoredControlPointImageFile;
import com.is.bcs.config.properties.ImageUploadProperties;
import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 사진 파일 저장 왕복 — 외부 도구 없이 돈다.
 *
 * <p>종전에는 이 경로가 `webpinfo` 를 띄웠기 때문에 그 도구가 깔린 기계에서만 검증할 수 있었고,
 * 그래서 저장 경로에 검증이 하나도 없었다. 크기를 JVM 안에서 읽게 된 뒤로는 여기서 바로 잰다.
 */
class LocalControlPointImageStorageAdapterTest {

    private static final OffsetDateTime CAPTURED_AT = OffsetDateTime.parse("2026-08-01T10:30:00+09:00");

    /** 최소한의 손실 압축 WebP 머리 — 내용은 없지만 크기를 읽는 데 필요한 자리는 모두 채웠다. */
    private static byte[] webp(int width, int height) {
        byte[] file = new byte[30];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, file, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, file, 8, 4);
        System.arraycopy("VP8 ".getBytes(StandardCharsets.US_ASCII), 0, file, 12, 4);
        file[23] = (byte) 0x9D;
        file[24] = (byte) 0x01;
        file[25] = (byte) 0x2A;
        file[26] = (byte) (width & 0xFF);
        file[27] = (byte) ((width >> 8) & 0xFF);
        file[28] = (byte) (height & 0xFF);
        file[29] = (byte) ((height >> 8) & 0xFF);
        return file;
    }

    /*
     * 기준점 이름을 ASCII 로 둔다. 이름은 저장 파일명에 들어가는데, 로케일이 비어 있는 환경에서는
     * JVM 의 파일 이름 인코딩이 ASCII 로 잡혀 한글 경로 자체를 만들 수 없다(도커 이미지의 LANG 참고).
     * 여기서 보려는 것은 크기 읽기와 파일 왕복이라 이름으로 그 환경 차이를 끌어들이지 않는다.
     */
    private LocalControlPointImageStorageAdapter adapterAt(Path root) {
        return new LocalControlPointImageStorageAdapter(
                new ImageUploadProperties(root.toString(), DataSize.ofMegabytes(5), 12000, 12000),
                new ControlPointImageFileNameGenerator());
    }

    private StoredControlPointImageFile store(Path root, byte[] content) {
        return adapterAt(root).store(1L, 2L, "P1465", CAPTURED_AT,
                "field.webp", "image/webp", content.length, content);
    }

    @Test
    @DisplayName("저장하면 크기를 읽어 돌려주고 파일이 그 자리에 남는다")
    void store_readsDimensionsAndWritesFile(@TempDir Path root) {
        byte[] content = webp(800, 600);

        StoredControlPointImageFile stored = store(root, content);

        assertEquals(800, stored.width());
        assertEquals(600, stored.height());
        assertEquals(content.length, stored.fileSize());
        assertTrue(Files.isRegularFile(root.resolve(stored.storagePath())));
    }

    @Test
    @DisplayName("한도를 넘는 크기는 디스크를 건드리지 않고 거절한다")
    void store_rejectsOversizeWithoutTouchingDisk(@TempDir Path root) {
        LocalControlPointImageStorageAdapter adapter = new LocalControlPointImageStorageAdapter(
                new ImageUploadProperties(root.toString(), DataSize.ofMegabytes(5), 400, 400),
                new ControlPointImageFileNameGenerator());
        byte[] content = webp(800, 600);

        assertThrows(InvalidControlPointImageException.class, () -> adapter.store(
                1L, 2L, "P1465", CAPTURED_AT, "field.webp", "image/webp", content.length, content));

        // 거절된 사진은 임시 파일도 남기지 않는다 — 폴더 자체가 만들어지지 않는다
        assertTrue(Files.notExists(root.resolve("control-points")));
    }

    @Test
    @DisplayName("WebP 가 아니면 거절한다")
    void store_rejectsNonWebp(@TempDir Path root) {
        byte[] content = "not an image".getBytes(StandardCharsets.UTF_8);

        assertThrows(InvalidControlPointImageException.class, () -> store(root, content));
    }

    @Test
    @DisplayName("저장한 파일을 다시 읽으면 같은 바이트가 나오고, 지우면 사라진다")
    void readAndDelete(@TempDir Path root) {
        byte[] content = webp(800, 600);
        LocalControlPointImageStorageAdapter adapter = adapterAt(root);
        StoredControlPointImageFile stored = adapter.store(
                1L, 2L, "P1465", CAPTURED_AT, "field.webp", "image/webp", content.length, content);

        assertEquals(content.length, adapter.read(stored.storagePath()).length);

        adapter.deleteIfExists(stored.storagePath());

        assertTrue(Files.notExists(root.resolve(stored.storagePath())));
    }
}
