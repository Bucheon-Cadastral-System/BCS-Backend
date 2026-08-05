package com.is.bcs.adapter.in.web.controlpoint;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기준점 API 계약 검증 — DB 필요(bcs/docker-compose). 데이터는 고객사 대상지 CSV 실측값. */
@SpringBootTest
@Transactional
class ControlPointApiTest {

    // 경위도는 보내지 않는다 — 서버가 성과(TM)에서 파생한다
    private static final String CSV_ROW1_JSON = """
            {
              "pointNo": "41192D000001265",
              "type": "DOGEUN",
              "name": "1465공",
              "crs": "GRS80_CENTRAL",
              "northing": 545236.77,
              "easting": 181840.96,
              "regionCode": "10300",
              "regionName": "춘의동",
              "address": "경기도 부천시 춘의동 102-16",
              "markerMaterial": "STEEL",
              "installType": "INSTALLED",
              "installedDate": "2018-02-21",
              "traverse": {"grade": "1", "intersection": false}
            }
            """;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 응답의 첫 id — 등록 응답은 point 가 먼저 실려 point.id 가 잡힌다. */
    private long extractId(String body) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(body);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    private MvcResult register() throws Exception {
        return mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CSV_ROW1_JSON))
                .andReturn();
    }

    @Test
    @DisplayName("등록 — 201과 Location, 서버가 파생한 경위도를 실은 리소스를 반환한다")
    void register_returns201WithLocation() throws Exception {
        MvcResult result = register();

        assertEquals(201, result.getResponse().getStatus());
        assertEquals("/api/control-points/41192D000001265", result.getResponse().getHeader("Location"));
        String body = bodyOf(result);
        assertTrue(body.contains("\"created\":true"));
        assertTrue(body.contains("\"pointNo\":\"41192D000001265\""));
        assertTrue(body.contains("\"northing\":545236.77"));
        assertTrue(body.contains("\"easting\":181840.96"));
        // 정답지 = 대상지 CSV 같은 행의 경위도 — 문자열 접두가 아니라 수치 오차로 본다
        assertEquals(126.794623, JsonPath.<Double>read(body, "$.point.longitude"), 1e-6);
        assertEquals(37.506423, JsonPath.<Double>read(body, "$.point.latitude"), 1e-6);
        assertTrue(body.contains("\"id\":"));
    }

    @Test
    @DisplayName("같은 점을 다시 등록 — 임포트와 같은 규칙이라 거부가 아니라 200(변경 없음)이다")
    void register_samePointAgain_returns200Unchanged() throws Exception {
        register();

        MvcResult result = register();

        assertEquals(200, result.getResponse().getStatus());
        String body = bodyOf(result);
        assertTrue(body.contains("\"created\":false"));
        assertTrue(body.contains("\"updated\":false"));
    }

    @Test
    @DisplayName("다른 이름의 점이 쓰는 관리번호 등록 — 409 CONTROL_POINT_DUPLICATE")
    void register_pointNoTakenByOtherName_409() throws Exception {
        register();

        MvcResult result = mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pointNo": "41192D000001265",
                                  "type": "DOGEUN",
                                  "name": "9999공",
                                  "crs": "GRS80_CENTRAL",
                                  "northing": 545000.00,
                                  "easting": 181000.00
                                }
                                """))
                .andReturn();

        assertEquals(409, result.getResponse().getStatus());
        assertTrue(bodyOf(result).contains("\"code\":\"CONTROL_POINT_DUPLICATE\""));
    }

    @Test
    @DisplayName("필수값 누락 등록 — 400 COMMON_INVALID_INPUT + errors[]")
    void register_missingFields_400() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DOGEUN\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"code\":\"COMMON_INVALID_INPUT\""));
        assertTrue(body.contains("\"errors\":"));
        assertTrue(body.contains("\"field\":\"pointNo\""));
    }

    @Test
    @DisplayName("수정 — 식별·성과가 바뀌고 경위도가 재파생되며, 다른 점의 관리번호로는 409")
    void updatePoint() throws Exception {
        long id = extractId(bodyOf(register()));

        MvcResult result = mockMvc.perform(put("/api/control-points/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pointNo": "41192D000012345", "type": "DOGEUN", "name": "1465공(이설)",
                                 "crs": "GRS80_CENTRAL", "northing": 545240.00, "easting": 181845.00}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String body = bodyOf(result);
        assertTrue(body.contains("\"pointNo\":\"41192D000012345\""));
        assertTrue(body.contains("\"name\":\"1465공(이설)\""));
        assertEquals(37.506, JsonPath.<Double>read(body, "$.point.latitude"), 1e-3); // 성과에서 재파생

        mockMvc.perform(put("/api/control-points/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pointNo": "41192D000000001", "type": "DOGEUN", "name": "이름",
                                 "crs": "GRS80_CENTRAL", "northing": 545000.00, "easting": 181000.00}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("삭제 — 조사에서 쓰지 않는 점은 204, 대상으로 지정된 점은 409 CONTROL_POINT_IN_USE")
    void deletePoint() throws Exception {
        long id = extractId(bodyOf(register()));

        // 조사 대상으로 지정하면 삭제가 거부된다 — 조사 데이터는 프로젝트 소유라 점 삭제가 지울 수 없다
        MvcResult project = mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "일제조사", "startedOn": "2026-07-01", "targetPointIds": [%d]}
                                """.formatted(id)))
                .andExpect(status().isCreated())
                .andReturn();
        long projectId = extractId(bodyOf(project));

        MvcResult blocked = mockMvc.perform(delete("/api/control-points/" + id))
                .andExpect(status().isConflict())
                .andReturn();
        assertTrue(bodyOf(blocked).contains("\"code\":\"CONTROL_POINT_IN_USE\""));

        // 프로젝트를 지워 참조가 사라지면 삭제된다
        mockMvc.perform(delete("/api/survey-projects/" + projectId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/control-points/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/control-points/41192D000001265")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/control-points/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("목록 — content로 감싼 전체 목록을 반환한다")
    void list_returnsContentWrapper() throws Exception {
        register();

        MvcResult result = mockMvc.perform(get("/api/control-points"))
                .andExpect(status().isOk())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"content\":["));
        assertTrue(body.contains("\"pointNo\":\"41192D000001265\""));
    }

    @Test
    @DisplayName("관리번호 단건 조회 — 있으면 200, 없으면 404 CONTROL_POINT_NOT_FOUND")
    void getByPointNo() throws Exception {
        register();

        MvcResult found = mockMvc.perform(get("/api/control-points/41192D000001265"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(found).contains("\"name\":\"1465공\""));

        MvcResult missing = mockMvc.perform(get("/api/control-points/41192D999999999"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(bodyOf(missing).contains("\"code\":\"CONTROL_POINT_NOT_FOUND\""));
    }
}
