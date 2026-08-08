package com.is.bcs.adapter.out.file;

import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** WebP 앞머리에서 가로·세로를 읽는 검증 — 외부 도구도 DB도 필요 없다. */
class WebpHeaderTest {

    /** 컨테이너 머리 20바이트 + 청크 내용. 청크 크기 칸은 이 검증이 보지 않으므로 0으로 둔다. */
    private static byte[] container(String chunk, byte... payload) {
        byte[] file = new byte[20 + payload.length];
        put(file, 0, "RIFF");
        put(file, 8, "WEBP");
        put(file, 12, chunk);
        System.arraycopy(payload, 0, file, 20, payload.length);
        return file;
    }

    private static void put(byte[] file, int at, String ascii) {
        byte[] bytes = ascii.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, file, at, bytes.length);
    }

    private static byte low(int value) {
        return (byte) (value & 0xFF);
    }

    private static byte high(int value) {
        return (byte) ((value >> 8) & 0xFF);
    }

    @Test
    @DisplayName("손실 압축(VP8) 키프레임에서 14비트 가로·세로를 읽는다")
    void lossy() {
        // 프레임 태그 3바이트(최하위 비트 0 = 키프레임) + 시작 코드 + 가로 800 + 세로 600
        byte[] file = container("VP8 ",
                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x9D, (byte) 0x01, (byte) 0x2A,
                low(800), high(800),
                low(600), high(600));

        WebpHeader.Dimensions dimensions = WebpHeader.read(file);

        assertEquals(800, dimensions.width());
        assertEquals(600, dimensions.height());
    }

    @Test
    @DisplayName("손실 압축에서 상위 2비트(확대 배율)는 크기로 읽지 않는다")
    void lossyIgnoresScale() {
        // 0x3FFF 위의 비트에 배율이 들어온다. 마스크하지 않으면 크기가 엉뚱한 값이 된다
        int widthWithScale = 800 | (0b10 << 14);
        byte[] file = container("VP8 ",
                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x9D, (byte) 0x01, (byte) 0x2A,
                low(widthWithScale), high(widthWithScale),
                low(600), high(600));

        assertEquals(800, WebpHeader.read(file).width());
    }

    @Test
    @DisplayName("무손실 압축(VP8L)은 1을 뺀 값이 적혀 있어 되돌려 읽는다")
    void lossless() {
        int bits = (800 - 1) | ((600 - 1) << 14);
        byte[] file = container("VP8L",
                (byte) 0x2F,
                (byte) (bits & 0xFF),
                (byte) ((bits >> 8) & 0xFF),
                (byte) ((bits >> 16) & 0xFF),
                (byte) ((bits >> 24) & 0xFF));

        WebpHeader.Dimensions dimensions = WebpHeader.read(file);

        assertEquals(800, dimensions.width());
        assertEquals(600, dimensions.height());
    }

    @Test
    @DisplayName("확장 형식(VP8X)은 24비트 화면 크기를 쓴다")
    void extended() {
        int width = 4000;
        int height = 3000;
        byte[] file = container("VP8X",
                (byte) 0x10, (byte) 0, (byte) 0, (byte) 0,
                (byte) ((width - 1) & 0xFF), (byte) (((width - 1) >> 8) & 0xFF), (byte) (((width - 1) >> 16) & 0xFF),
                (byte) ((height - 1) & 0xFF), (byte) (((height - 1) >> 8) & 0xFF), (byte) (((height - 1) >> 16) & 0xFF));

        WebpHeader.Dimensions dimensions = WebpHeader.read(file);

        assertEquals(4000, dimensions.width());
        assertEquals(3000, dimensions.height());
    }

    @Test
    @DisplayName("RIFF·WEBP 머리가 아니면 거부한다")
    void rejectsForeignFile() {
        byte[] png = new byte[40];
        put(png, 0, "PNG");

        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(png));
    }

    @Test
    @DisplayName("아는 청크가 아니면 거부한다 — 크기를 읽을 자리가 없다")
    void rejectsUnknownChunk() {
        byte[] file = container("XXXX", new byte[10]);

        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(file));
    }

    @Test
    @DisplayName("머리가 잘려 있으면 거부한다")
    void rejectsTruncated() {
        byte[] file = container("VP8 ", (byte) 0x00, (byte) 0x00);

        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(file));
    }

    @Test
    @DisplayName("손실 압축인데 키프레임이 아니거나 시작 코드가 틀리면 거부한다")
    void rejectsBrokenLossy() {
        byte[] notKeyFrame = container("VP8 ",
                (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x9D, (byte) 0x01, (byte) 0x2A,
                low(800), high(800), low(600), high(600));
        byte[] badStartCode = container("VP8 ",
                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x01, (byte) 0x2A,
                low(800), high(800), low(600), high(600));

        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(notKeyFrame));
        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(badStartCode));
    }

    @Test
    @DisplayName("무손실 압축인데 서명이 틀리면 거부한다")
    void rejectsBrokenLossless() {
        byte[] file = container("VP8L", (byte) 0x00, (byte) 0, (byte) 0, (byte) 0, (byte) 0);

        assertThrows(InvalidControlPointImageException.class, () -> WebpHeader.read(file));
    }
}
