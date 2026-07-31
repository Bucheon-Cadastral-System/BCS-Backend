package com.is.bcs.adapter.in.web.survey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 조사 프로젝트·조사기록 API 계약 검증 — DB 필요(bcs/docker-compose). */
@SpringBootTest
@Transactional
class SurveyApiTest {

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

    private long extractId(String body) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(body);
        assertTrue(m.find());
        return Long.parseLong(m.group(1));
    }

    private long createProject() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "2026 일제조사", "startedOn": "2026-07-01", "note": "정기 조사"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(bodyOf(result));
    }

    private long registerPoint() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pointNo": "41192D000001265", "type": "DOGEUN", "name": "1465공",
                                 "crs": "GRS80_CENTRAL", "northing": 545236.77, "easting": 181840.96,
                                 "longitude": 126.794623, "latitude": 37.506423}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(bodyOf(result));
    }

    /** 실파일 임포트 — 프로젝트 49대상·44조사가 생기고 projectId를 돌려준다. */
    private long importSample() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            MvcResult result = mockMvc.perform(multipart("/api/imports/survey-csv")
                            .file(new MockMultipartFile("file", "대상지.csv", "text/csv", in.readAllBytes()))
                            .param("name", "2026 일제조사").param("startedOn", "2026-07-01"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Matcher m = Pattern.compile("\"projectId\":(\\d+)").matcher(bodyOf(result));
            assertTrue(m.find());
            return Long.parseLong(m.group(1));
        }
    }

    @Test
    @DisplayName("조사 대상 목록 — 임포트한 프로젝트의 대상 49건을 돌려준다")
    void listTargets() throws Exception {
        long projectId = importSample();

        MvcResult result = mockMvc.perform(get("/api/survey-projects/" + projectId + "/targets"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(bodyOf(result).contains("\"content\":["));
        assertEquals(49, bodyOf(result).split(",").length); // 대상 49건

        mockMvc.perform(get("/api/survey-projects/999999/targets")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("프로젝트 생성·목록·단건 조회")
    void createAndGetProjects() throws Exception {
        long id = createProject();

        MvcResult list = mockMvc.perform(get("/api/survey-projects"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(list).contains("\"content\":["));
        assertTrue(bodyOf(list).contains("\"name\":\"2026 일제조사\""));

        MvcResult single = mockMvc.perform(get("/api/survey-projects/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(single).contains("\"startedOn\":\"2026-07-01\""));

        mockMvc.perform(get("/api/survey-projects/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("조사기록 — 기록은 200, 재기록은 정정, 목록은 content로 반환")
    void recordAndReviseAndList() throws Exception {
        long projectId = createProject();
        long pointId = registerPoint();

        MvcResult first = mockMvc.perform(
                        put("/api/survey-projects/" + projectId + "/records/" + pointId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"result\": \"INTACT\", \"note\": \"대상(2건)\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String firstBody = bodyOf(first);
        assertTrue(firstBody.contains("\"result\":\"INTACT\""));
        assertTrue(firstBody.contains("+09:00")); // surveyedAt KST offset

        MvcResult revised = mockMvc.perform(
                        put("/api/survey-projects/" + projectId + "/records/" + pointId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"result\": \"LOST\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(revised).contains("\"result\":\"LOST\""));
        assertEquals(extractId(firstBody), extractId(bodyOf(revised))); // 새 레코드가 아니라 정정

        MvcResult list = mockMvc.perform(get("/api/survey-projects/" + projectId + "/records"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(list).contains("\"content\":["));
        assertTrue(bodyOf(list).contains("\"pointId\":" + pointId));
    }

    @Test
    @DisplayName("조사기록 삭제는 204, 없는 기록 삭제·없는 프로젝트 기록은 404")
    void deleteRecord() throws Exception {
        long projectId = createProject();
        long pointId = registerPoint();
        mockMvc.perform(put("/api/survey-projects/" + projectId + "/records/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\": \"INTACT\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/survey-projects/" + projectId + "/records/" + pointId))
                .andExpect(status().isNoContent());

        MvcResult again = mockMvc.perform(
                        delete("/api/survey-projects/" + projectId + "/records/" + pointId))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(bodyOf(again).contains("\"code\":\"SURVEY_RECORD_NOT_FOUND\""));

        mockMvc.perform(put("/api/survey-projects/999999/records/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\": \"INTACT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("결과 없는 조사기록 요청 — 400 COMMON_INVALID_INPUT")
    void record_missingResult_400() throws Exception {
        long projectId = createProject();
        long pointId = registerPoint();

        MvcResult result = mockMvc.perform(
                        put("/api/survey-projects/" + projectId + "/records/" + pointId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(bodyOf(result).contains("\"code\":\"COMMON_INVALID_INPUT\""));
    }

    @Test
    @DisplayName("진행률 — 임포트한 프로젝트의 대상 49·조사 44·미조사 5, 완료 아님")
    void getProgress_reportsTargetScopedCounts() throws Exception {
        long projectId = importSample();

        MvcResult result = mockMvc.perform(get("/api/survey-projects/" + projectId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();
        String body = bodyOf(result);
        assertTrue(body.contains("\"totalPoints\":49"));
        assertTrue(body.contains("\"surveyedPoints\":44"));
        assertTrue(body.contains("\"notSurveyedPoints\":5"));
        assertTrue(body.contains("\"complete\":false"));
    }
}
