package com.is.bcs.adapter.in.web.survey;

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
                                {"type": "EXCAVATION_CONSULTATION", "name": "2026 굴착협의", "note": "협의번호 2333"}
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

    @Test
    @DisplayName("프로젝트 생성·목록·단건 조회")
    void createAndGetProjects() throws Exception {
        long id = createProject();

        MvcResult list = mockMvc.perform(get("/api/survey-projects"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(list).contains("\"content\":["));
        assertTrue(bodyOf(list).contains("\"name\":\"2026 굴착협의\""));

        MvcResult single = mockMvc.perform(get("/api/survey-projects/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(single).contains("\"type\":\"EXCAVATION_CONSULTATION\""));

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
}
