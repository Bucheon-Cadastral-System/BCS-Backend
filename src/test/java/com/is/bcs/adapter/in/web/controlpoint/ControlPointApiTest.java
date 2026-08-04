package com.is.bcs.adapter.in.web.controlpoint;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        // 정답지 = 대상지 CSV 같은 행의 경위도(126.794623, 37.506423) — 파생 오차가 소수 4자리 안이다
        assertTrue(body.contains("\"longitude\":126.7946"), body);
        assertTrue(body.contains("\"latitude\":37.5064"), body);
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
