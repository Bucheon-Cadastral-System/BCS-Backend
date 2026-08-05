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

    /** 대상 없는 프로젝트는 만들 수 없으므로 등록해 둔 점 하나를 대상으로 넣는다. */
    private long createProject(long targetPointId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "2026 일제조사", "startedOn": "2026-07-01", "note": "정기 조사",
                                 "targetPointIds": [%d]}
                                """.formatted(targetPointId)))
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

        String body = bodyOf(result);
        assertTrue(body.contains("\"content\":["));
        // 응답 전체의 콤마를 세면 필드가 하나 늘 때마다 값이 달라진다 — 목록 안만 잘라 센다
        String content = body.substring(body.indexOf('[') + 1, body.lastIndexOf(']'));
        assertEquals(49, content.split(",").length);

        mockMvc.perform(get("/api/survey-projects/999999/targets")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("프로젝트 생성·목록·단건 조회 — 생성 시 지정한 대상이 대상 목록에 실린다")
    void createAndGetProjects() throws Exception {
        long pointId = registerPoint();
        long id = createProject(pointId);

        MvcResult list = mockMvc.perform(get("/api/survey-projects"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(list).contains("\"content\":["));
        assertTrue(bodyOf(list).contains("\"name\":\"2026 일제조사\""));
        // 목록은 요약이다 — 행별 완료 표시·작성자 표기가 여기 실려 온다(작성자는 인증 전이라 null)
        assertTrue(bodyOf(list).contains("\"targetCount\":1"));
        assertTrue(bodyOf(list).contains("\"surveyedCount\":0"));
        assertTrue(bodyOf(list).contains("\"authorName\":null"));

        MvcResult single = mockMvc.perform(get("/api/survey-projects/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(single).contains("\"startedOn\":\"2026-07-01\""));

        MvcResult targets = mockMvc.perform(get("/api/survey-projects/" + id + "/targets"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(targets).contains(String.valueOf(pointId)));

        mockMvc.perform(get("/api/survey-projects/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대상 없는 생성은 400, 없는 점을 대상으로 지정하면 404")
    void create_withoutTargetsOrMissingPoint() throws Exception {
        MvcResult empty = mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-07-01\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(bodyOf(empty).contains("\"code\":\"COMMON_INVALID_INPUT\""));

        MvcResult missing = mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-07-01\", \"targetPointIds\": [999999]}"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(bodyOf(missing).contains("\"code\":\"CONTROL_POINT_NOT_FOUND\""));

        // null 요소는 요청 계약에서 걸러 400 — 흘려보내면 조회 단계에서 5xx 로 둔갑한다
        mockMvc.perform(post("/api/survey-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-07-01\", \"targetPointIds\": [null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대상이 아닌 점에 기록 — 404 SURVEY_TARGET_NOT_FOUND")
    void record_nonTargetPoint_404() throws Exception {
        long targetPointId = registerPoint();
        long projectId = createProject(targetPointId);
        MvcResult other = mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pointNo": "41192D000009998", "type": "DOGEUN", "name": "9998공",
                                 "crs": "GRS80_CENTRAL", "northing": 545100.00, "easting": 181100.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long otherPointId = extractId(bodyOf(other));

        MvcResult result = mockMvc.perform(
                        put("/api/survey-projects/" + projectId + "/records/" + otherPointId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"result\": \"INTACT\"}"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(bodyOf(result).contains("\"code\":\"SURVEY_TARGET_NOT_FOUND\""));
    }

    @Test
    @DisplayName("프로젝트 수정 — 이름·기간·비고·대상이 바뀌고, 대상에서 빠진 점의 기록은 함께 지워진다")
    void updateProject_reassignsTargets() throws Exception {
        long pointId = registerPoint();
        long id = createProject(pointId);
        MvcResult other = mockMvc.perform(post("/api/control-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pointNo": "41192D000009997", "type": "DOGEUN", "name": "9997공",
                                 "crs": "GRS80_CENTRAL", "northing": 545100.00, "easting": 181100.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long otherPointId = extractId(bodyOf(other));
        // 빠질 점에 기록을 남겨 둔다 — 재지정이 이 기록을 함께 지우는지 본다
        mockMvc.perform(put("/api/survey-projects/" + id + "/records/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\": \"INTACT\"}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(put("/api/survey-projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "하반기 조사", "startedOn": "2026-08-01", "endedOn": "2026-08-31",
                                 "targetPointIds": [%d]}
                                """.formatted(otherPointId)))
                .andExpect(status().isOk())
                .andReturn();
        String body = bodyOf(result);
        assertTrue(body.contains("\"name\":\"하반기 조사\""));
        assertTrue(body.contains("\"startedOn\":\"2026-08-01\""));
        assertTrue(body.contains("\"endedOn\":\"2026-08-31\""));
        assertTrue(body.contains("\"note\":null")); // 보낸 대로 — 비고를 비웠다

        // 대상은 재지정한 점만 남는다
        MvcResult targets = mockMvc.perform(get("/api/survey-projects/" + id + "/targets"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(targets).contains("\"content\":[" + otherPointId + "]"));

        // 빠진 점의 기록도 지워졌다 — 남기면 어느 화면에도 닿지 않으면서 그 점의 삭제만 막는다
        MvcResult records = mockMvc.perform(get("/api/survey-projects/" + id + "/records"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(records).contains("\"content\":[]"));
        mockMvc.perform(delete("/api/control-points/" + pointId)).andExpect(status().isNoContent());

        mockMvc.perform(put("/api/survey-projects/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-08-01\", \"targetPointIds\": [%d]}"
                                .formatted(otherPointId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대상 없는 수정은 400 — 생성과 같은 규칙(최소 1점, null 요소 거부)")
    void updateProject_withoutTargets_400() throws Exception {
        long pointId = registerPoint();
        long id = createProject(pointId);

        MvcResult missing = mockMvc.perform(put("/api/survey-projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-08-01\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(bodyOf(missing).contains("\"code\":\"COMMON_INVALID_INPUT\""));

        mockMvc.perform(put("/api/survey-projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"이름\", \"startedOn\": \"2026-08-01\", \"targetPointIds\": [null]}"))
                .andExpect(status().isBadRequest());

        // 거부된 수정은 아무것도 남기지 않는다 — 대상 그대로
        MvcResult targets = mockMvc.perform(get("/api/survey-projects/" + id + "/targets"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(targets).contains("\"content\":[" + pointId + "]"));
    }

    @Test
    @DisplayName("프로젝트 삭제 뒤에는 그 점을 지울 수 있다 — 대상·기록 행이 실제로 사라졌다는 교차 검증")
    void deleteProject_freesPointReferences() throws Exception {
        long pointId = registerPoint();
        long projectId = createProject(pointId);
        mockMvc.perform(put("/api/survey-projects/" + projectId + "/records/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\": \"INTACT\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/survey-projects/" + projectId)).andExpect(status().isNoContent());

        // 기준점 삭제는 대상·기록 참조가 남아 있으면 409 로 막힌다 — 204 는 두 참조가 실제로 지워졌다는 뜻이다
        mockMvc.perform(delete("/api/control-points/" + pointId)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("프로젝트 삭제 — 대상·기록까지 지워지고 204, 없는 프로젝트 삭제는 404")
    void deleteProject_cascades() throws Exception {
        long projectId = importSample(); // 대상 49·기록 44가 실린 프로젝트 — 동반 삭제를 실데이터로 검증

        mockMvc.perform(delete("/api/survey-projects/" + projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/survey-projects/" + projectId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/survey-projects/" + projectId + "/targets")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/survey-projects/" + projectId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("조사기록 — 기록은 200, 재기록은 정정, 목록은 content로 반환")
    void recordAndReviseAndList() throws Exception {
        long pointId = registerPoint();
        long projectId = createProject(pointId);

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
        long pointId = registerPoint();
        long projectId = createProject(pointId);
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
        long pointId = registerPoint();
        long projectId = createProject(pointId);

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
