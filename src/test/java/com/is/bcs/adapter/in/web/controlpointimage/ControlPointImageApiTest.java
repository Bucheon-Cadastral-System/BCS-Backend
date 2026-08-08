package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyTarget;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기준점 사진 등록·조회·내려받기 API 계약 검증 — DB 필요(bcs/docker-compose).
 *
 * <p>실제 저장 어댑터를 그대로 태운다. 크기 확인이 JVM 안에서 끝나므로 외부 도구가 필요 없다.
 * 파일은 `build/` 아래에 떨어져 `gradle clean` 과 함께 정리된다(트랜잭션이 되돌아갈 때도 지워진다).
 */
@SpringBootTest(properties = "app.image-upload.root-directory=build/test-uploads")
@Transactional
class ControlPointImageApiTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-08-01T10:00:00+09:00");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SaveControlPointPort savePointPort;

    @Autowired
    private SaveSurveyProjectPort saveProjectPort;

    @Autowired
    private SaveSurveyTargetPort saveTargetPort;

    @Autowired
    private SaveMemberPort saveMemberPort;

    private MockMvc mockMvc;

    private int seq = 0;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /**
     * 손실 압축 WebP 최소본.
     *
     * <p>우리 리더는 컨테이너 길이와 청크 길이를 읽지 않지만 여기서는 실제 바이트 수에 맞춰 채운다.
     * 성공 경로의 표본이 명세를 어기고 있으면, 나중에 그 길이를 보게 됐을 때 멀쩡한 검증이 먼저 깨진다.
     */
    private static byte[] webp(int width, int height) {
        byte[] file = new byte[30];
        ascii(file, 0, "RIFF");
        little32(file, 4, file.length - 8);
        ascii(file, 8, "WEBP");
        ascii(file, 12, "VP8 ");
        little32(file, 16, file.length - 20);
        file[23] = (byte) 0x9D;
        file[24] = (byte) 0x01;
        file[25] = (byte) 0x2A;
        file[26] = (byte) (width & 0xFF);
        file[27] = (byte) ((width >> 8) & 0xFF);
        file[28] = (byte) (height & 0xFF);
        file[29] = (byte) ((height >> 8) & 0xFF);
        return file;
    }

    private static void ascii(byte[] file, int at, String value) {
        System.arraycopy(value.getBytes(StandardCharsets.US_ASCII), 0, file, at, 4);
    }

    private static void little32(byte[] file, int at, int value) {
        file[at] = (byte) (value & 0xFF);
        file[at + 1] = (byte) ((value >> 8) & 0xFF);
        file[at + 2] = (byte) ((value >> 16) & 0xFF);
        file[at + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private RequestPostProcessor as(long memberId) {
        AccessTokenClaims claims =
                new AccessTokenClaims(memberId, MemberRole.USER, Instant.now(), Instant.now().plusSeconds(900));
        return authentication(new UsernamePasswordAuthenticationToken(claims, "n/a", List.of()));
    }

    private long memberId() {
        seq++;
        Member member = Member.registerWithKakao("kakao-img-api-" + seq, AT);
        member.completeProfile("조사원" + seq, "0101111%04d".formatted(seq), "imgapi" + seq + "@example.com",
                District.WONMI, "민원지적과", Team.CADASTRAL_MANAGEMENT, Position.OFFICER);
        member.approve(AT);
        return saveMemberPort.save(member).getId();
    }

    /* 기준점 이름이 저장 파일명에 들어간다. 로케일이 비어 있는 환경에서는 한글 경로를 만들 수 없으므로
       이름을 ASCII 로 둔다(도커 이미지의 LANG 참고 — 그쪽은 설정으로 막았고 여기서는 조건을 끌어들이지 않는다). */
    private long pointId() {
        seq++;
        return savePointPort.save(ControlPoint.register(
                "41192I%09d".formatted(seq), PointType.DOGEUN, "IMG" + seq,
                new TmCoordinate(CoordinateSystem.GRS80_CENTRAL,
                        new BigDecimal("545236.77"), new BigDecimal("181840.96")),
                new GeoCoordinate(126.794623, 37.506423),
                "10300", "춘의동", "경기도 부천시 춘의동 102-16",
                MarkerMaterial.STEEL, InstallType.INSTALLED, LocalDate.of(2018, 2, 21),
                new TraverseInfo("1", null, null, false), null, null)).getId();
    }

    private long projectWithTarget(long pointId) {
        SurveyProject project = saveProjectPort.save(
                SurveyProject.create(null, "2026 일제조사", LocalDate.of(2026, 7, 1), null, null));
        saveTargetPort.save(SurveyTarget.create(project.getId(), pointId));
        return project.getId();
    }

    private ResultActions upload(long projectId, long pointId, long uploaderId, String result, String capturedAt)
            throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "field.webp", "image/webp", webp(800, 600));
        return mockMvc.perform(MockMvcRequestBuilders
                .multipart("/api/survey-projects/{projectId}/control-points/{pointId}/image", projectId, pointId)
                .file(image)
                .param("capturedAt", capturedAt)
                .param("result", result)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .with(as(uploaderId)));
    }

    private long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
        return id.longValue();
    }

    @Test
    @DisplayName("사진을 올리면 201, 같은 자리에 다시 올리면 200 이다 — 자리는 회차마다 한 장이다")
    void upload_createsThenReplaces() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);
        long uploaderId = memberId();

        upload(projectId, pointId, uploaderId, "INTACT", "2026-08-01T10:30:00+09:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId", is((int) projectId)))
                .andExpect(jsonPath("$.controlPointId", is((int) pointId)))
                // 크기는 서버가 파일 앞머리에서 읽은 값이다 — 클라이언트가 알려 온 값이 아니다
                .andExpect(jsonPath("$.width", is(800)))
                .andExpect(jsonPath("$.height", is(600)));

        upload(projectId, pointId, uploaderId, "LOST", "2026-08-02T10:30:00+09:00")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("한 기준점이 여러 회차의 대상이면 회차마다 사진이 따로 남는다")
    void image_isPerProjectAndPoint() throws Exception {
        long pointId = pointId();
        long firstProject = projectWithTarget(pointId);
        long secondProject = projectWithTarget(pointId);
        long uploaderId = memberId();

        upload(firstProject, pointId, uploaderId, "INTACT", "2026-08-01T10:30:00+09:00")
                .andExpect(status().isCreated());
        upload(secondProject, pointId, uploaderId, "LOST", "2026-08-05T10:30:00+09:00")
                .andExpect(status().isCreated());

        long first = idOf(mockMvc.perform(
                        get("/api/survey-projects/{projectId}/control-points/{pointId}/image", firstProject, pointId)
                                .with(as(uploaderId)))
                .andExpect(status().isOk())
                .andReturn());
        long second = idOf(mockMvc.perform(
                        get("/api/survey-projects/{projectId}/control-points/{pointId}/image", secondProject, pointId)
                                .with(as(uploaderId)))
                .andExpect(status().isOk())
                .andReturn());

        // 회차가 다르면 다른 사진이다 — 이 점의 사진이 한 장으로 합쳐지면 앞 회차의 현장이 사라진다
        assertNotEquals(first, second);

        // 점으로 모아 보면 두 장이 함께 나온다
        mockMvc.perform(get("/api/control-points/{pointId}/images", pointId).with(as(uploaderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].projectId", hasItems((int) firstProject, (int) secondProject)));
    }

    @Test
    @DisplayName("사진이 없는 자리는 404 다 — 화면이 없음과 실패를 가른다")
    void getByProjectAndPoint_missingIs404() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);

        mockMvc.perform(get("/api/survey-projects/{projectId}/control-points/{pointId}/image", projectId, pointId)
                        .with(as(memberId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("회차별·전체 목록으로도 볼 수 있다")
    void listByProjectAndAll() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);
        long uploaderId = memberId();
        upload(projectId, pointId, uploaderId, "INTACT", "2026-08-01T10:30:00+09:00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/survey-projects/{projectId}/images", projectId).with(as(uploaderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].controlPointId", is((int) pointId)));

        mockMvc.perform(get("/api/control-point-images").with(as(uploaderId)))
                .andExpect(status().isOk())
                // 올린 사람이 고른 이름은 메타데이터로만 남는다 — 저장 파일명은 따로다
                .andExpect(jsonPath("$.content[*].originalFileName", hasItem("field.webp")));
    }

    @Test
    @DisplayName("올린 사진을 그대로 내려 주고, 내려받기는 파일 이름을 함께 준다")
    void fileAndDownload() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);
        long uploaderId = memberId();
        long imageId = idOf(upload(projectId, pointId, uploaderId, "INTACT", "2026-08-01T10:30:00+09:00")
                .andExpect(status().isCreated())
                .andReturn());

        MvcResult view = mockMvc.perform(get("/api/control-point-images/{imageId}/file", imageId)
                        .with(as(uploaderId)))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult download = mockMvc.perform(get("/api/control-point-images/{imageId}/download", imageId)
                        .with(as(uploaderId)))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(30, view.getResponse().getContentAsByteArray().length);
        assertEquals(MediaType.IMAGE_JPEG_VALUE.replace("jpeg", "webp"), view.getResponse().getContentType());
        // 저장 파일명의 UUID 는 사용자에게 보여 줄 것이 아니다 — 헤더의 이름에 섞여 나오면 안 된다
        String disposition = download.getResponse().getHeader("Content-Disposition");
        assertTrue(disposition != null && disposition.contains("filename"), disposition);
    }

    @Test
    @DisplayName("판정 없이 올리면 400 이다 — 사진만 남고 판정이 빠지는 길을 두지 않는다")
    void upload_requiresResult() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);
        MockMultipartFile image = new MockMultipartFile("image", "field.webp", "image/webp", webp(800, 600));

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/survey-projects/{projectId}/control-points/{pointId}/image", projectId, pointId)
                        .file(image)
                        .param("capturedAt", "2026-08-01T10:30:00+09:00")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(as(memberId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("WebP 가 아니면 400 이다")
    void upload_rejectsNonWebp() throws Exception {
        long pointId = pointId();
        long projectId = projectWithTarget(pointId);
        MockMultipartFile image = new MockMultipartFile(
                "image", "field.png", "image/png", "not a webp".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/survey-projects/{projectId}/control-points/{pointId}/image", projectId, pointId)
                        .file(image)
                        .param("capturedAt", "2026-08-01T10:30:00+09:00")
                        .param("result", "INTACT")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(as(memberId())))
                .andExpect(status().isBadRequest());
    }
}
