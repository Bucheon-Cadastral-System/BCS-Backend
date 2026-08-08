package com.is.bcs.adapter.out.file;

import com.is.bcs.domain.controlpointimage.exception.InvalidControlPointImageException;

/**
 * WebP 파일 앞머리에서 가로·세로를 읽는다.
 *
 * <p>WebP 는 RIFF 컨테이너다. 앞 12바이트가 컨테이너 머리(`RIFF` + 크기 + `WEBP`)이고 그다음 청크의
 * 종류에 따라 크기가 적힌 자리가 갈린다. 세 종류뿐이라 바이트를 직접 읽는다.
 *
 * <pre>
 *   0   RIFF
 *   4   파일 크기 - 8 (little endian)
 *   8   WEBP
 *  12   청크 종류 — "VP8 "(손실) · "VP8L"(무손실) · "VP8X"(확장)
 *  16   청크 크기
 *  20   청크 내용
 * </pre>
 *
 * <p>이 값을 외부 명령(webpinfo)으로 읽던 때가 있었다. 업로드 한 건마다 프로세스를 띄우고 그 출력을
 * 정규식으로 긁었는데, 그 도구가 실행 환경에 깔려 있어야 동작한다는 전제가 붙는다. 도커 이미지에는
 * 넣어 두었지만 개발 기계에는 없어서 로컬에서만 500 이 나는 일이 반복됐고, 테스트에서도 저장 경로를
 * 대역으로 세워야 했다. 형식이 명세로 정해져 있고 읽을 자리가 세 곳뿐이라 여기서 직접 읽는다.
 *
 * <p>크기를 읽는 일이 곧 형식 검사다. 세 청크 중 어느 것도 아니거나 앞머리가 잘려 있으면 읽을 자리가
 * 없으므로 거부한다. 확장자나 Content-Type 이 아니라 파일 내용이 근거다.
 */
final class WebpHeader {

    /** 컨테이너 머리 12바이트 + 청크 머리 8바이트. 이보다 짧으면 청크 내용이 시작되지도 않는다. */
    private static final int CHUNK_CONTENT = 20;

    /** 손실·무손실이 쓰는 14비트 크기 필드의 마스크 */
    private static final int SIZE_14_BITS = 0x3FFF;

    private WebpHeader() {
    }

    /**
     * @throws InvalidControlPointImageException WebP 가 아니거나 크기를 읽을 자리가 없을 때
     */
    static Dimensions read(byte[] content) {
        requireContainer(content);

        String chunk = ascii(content, 12, 4);

        return switch (chunk) {
            case "VP8 " -> lossy(content);
            case "VP8L" -> lossless(content);
            case "VP8X" -> extended(content);
            default -> throw new InvalidControlPointImageException("올바른 WebP 파일이 아닙니다.");
        };
    }

    private static void requireContainer(byte[] content) {
        if (content == null || content.length < CHUNK_CONTENT
                || !"RIFF".equals(ascii(content, 0, 4))
                || !"WEBP".equals(ascii(content, 8, 4))) {
            throw new InvalidControlPointImageException("올바른 WebP 파일이 아닙니다.");
        }
    }

    /**
     * 손실 압축(VP8) — 청크 내용이 키프레임 머리다.
     *
     * <pre>
     *  +0  프레임 태그 3바이트 (최하위 비트가 0 이면 키프레임)
     *  +3  시작 코드 0x9D 0x01 0x2A
     *  +6  가로 14비트 (상위 2비트는 확대 배율)
     *  +8  세로 14비트
     * </pre>
     *
     * <p>키프레임만 받는다. 정지 화상 WebP 의 첫 프레임은 언제나 키프레임이고, 그것이 아니면
     * 크기가 적힌 자리를 신뢰할 수 없다.
     */
    private static Dimensions lossy(byte[] content) {
        require(content, CHUNK_CONTENT + 10);

        boolean keyFrame = (content[CHUNK_CONTENT] & 1) == 0;
        boolean startCode = unsigned(content[CHUNK_CONTENT + 3]) == 0x9D
                && unsigned(content[CHUNK_CONTENT + 4]) == 0x01
                && unsigned(content[CHUNK_CONTENT + 5]) == 0x2A;

        if (!keyFrame || !startCode) {
            throw new InvalidControlPointImageException("손상되었거나 올바르지 않은 WebP 파일입니다.");
        }

        return new Dimensions(
                little16(content, CHUNK_CONTENT + 6) & SIZE_14_BITS,
                little16(content, CHUNK_CONTENT + 8) & SIZE_14_BITS);
    }

    /**
     * 무손실 압축(VP8L) — 서명 한 바이트 뒤에 가로·세로가 각각 14비트로 붙어 있다.
     * 적히는 값은 실제 크기에서 1을 뺀 것이다(0 픽셀이 없으므로 한 값을 아낀다).
     */
    private static Dimensions lossless(byte[] content) {
        require(content, CHUNK_CONTENT + 5);

        if (unsigned(content[CHUNK_CONTENT]) != 0x2F) {
            throw new InvalidControlPointImageException("손상되었거나 올바르지 않은 WebP 파일입니다.");
        }

        int bits = little32(content, CHUNK_CONTENT + 1);

        return new Dimensions(
                (bits & SIZE_14_BITS) + 1,
                ((bits >>> 14) & SIZE_14_BITS) + 1);
    }

    /**
     * 확장 형식(VP8X) — 알파·애니메이션 같은 기능 표시가 붙는 형식이고,
     * 화면 크기를 24비트로 따로 적어 둔다. 여기서도 적히는 값은 실제보다 1 작다.
     *
     * <pre>
     *  +0  기능 표시 1바이트
     *  +1  예약 3바이트
     *  +4  가로 - 1 (24비트)
     *  +7  세로 - 1 (24비트)
     * </pre>
     */
    private static Dimensions extended(byte[] content) {
        require(content, CHUNK_CONTENT + 10);

        return new Dimensions(
                little24(content, CHUNK_CONTENT + 4) + 1,
                little24(content, CHUNK_CONTENT + 7) + 1);
    }

    private static void require(byte[] content, int length) {
        if (content.length < length) {
            throw new InvalidControlPointImageException("WebP 이미지 크기를 확인할 수 없습니다.");
        }
    }

    private static String ascii(byte[] content, int from, int length) {
        StringBuilder text = new StringBuilder(length);
        for (int at = from; at < from + length; at++) {
            text.append((char) unsigned(content[at]));
        }
        return text.toString();
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }

    private static int little16(byte[] content, int at) {
        return unsigned(content[at]) | (unsigned(content[at + 1]) << 8);
    }

    private static int little24(byte[] content, int at) {
        return little16(content, at) | (unsigned(content[at + 2]) << 16);
    }

    private static int little32(byte[] content, int at) {
        return little24(content, at) | (unsigned(content[at + 3]) << 24);
    }

    record Dimensions(int width, int height) {
    }
}
