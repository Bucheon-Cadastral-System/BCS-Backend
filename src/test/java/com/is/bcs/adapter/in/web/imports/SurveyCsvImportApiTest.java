package com.is.bcs.adapter.in.web.imports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 대상지 CSV 임포트 API 검증 — DB 필요(bcs/docker-compose). 고객사 실파일 업로드로 끝까지 확인한다. */
@SpringBootTest
@Transactional
class SurveyCsvImportApiTest {

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

    private MockMultipartFile sampleFile() throws Exception {
        try (var in = getClass().getResourceAsStream("/survey-target-sample.csv")) {
            return new MockMultipartFile("file", "대상지.csv", "text/csv", in.readAllBytes());
        }
    }

    @Test
    @DisplayName("미리보기 — 200과 건수·열 매핑을 돌려주고 아무것도 등록하지 않는다")
    void preview_readsWithoutImporting() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/imports/survey-csv/preview").file(sampleFile()))
                .andExpect(status().isOk())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"totalRows\":49"), body);
        assertTrue(body.contains("\"기존조사내\":\"기존조사내용\""), body);
        assertTrue(body.contains("\"errors\":[]"), body);

        // 미리보기는 조사 프로젝트를 만들지 않는다
        MvcResult projects = mockMvc.perform(get("/api/survey-projects")).andExpect(status().isOk()).andReturn();
        assertTrue(bodyOf(projects).contains("\"content\":[]"), bodyOf(projects));
    }

    @Test
    @DisplayName("실파일 업로드 — 201과 요약(49점·조사 44건), 프로젝트·기준점·기록이 조회된다")
    void importRealFile_endToEnd() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(sampleFile())
                        .param("name", "2026 일제조사")
                        .param("note", "정기 조사")
                        .param("startedOn", "2026-07-01"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = bodyOf(result);
        assertTrue(body.contains("\"totalRows\":49"));
        assertTrue(body.contains("\"newPoints\":49"));
        assertTrue(body.contains("\"existingPoints\":0"));
        assertTrue(body.contains("\"createdRecords\":44"));

        Matcher m = Pattern.compile("\"projectId\":(\\d+)").matcher(body);
        assertTrue(m.find());
        String projectId = m.group(1);
        assertEquals("/api/survey-projects/" + projectId, result.getResponse().getHeader("Location"));

        // 임포트 결과가 실제로 조회 API에 반영됐는지
        MvcResult project = mockMvc.perform(get("/api/survey-projects/" + projectId))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(project).contains("\"startedOn\":\"2026-07-01\""));

        MvcResult points = mockMvc.perform(get("/api/control-points"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(49, countOf(bodyOf(points), "\"pointNo\":"));

        MvcResult records = mockMvc.perform(get("/api/survey-projects/" + projectId + "/records"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(44, countOf(bodyOf(records), "\"pointId\":"));
        assertTrue(bodyOf(records).contains("2025-09-08T00:00:00+09:00")); // 기존조사일의 KST 자정
    }

    @Test
    @DisplayName("같은 파일을 다시 임포트하면 기준점은 재사용되고 새 프로젝트만 생긴다")
    void importTwice_reusesPoints() throws Exception {
        mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(sampleFile()).param("name", "1차").param("startedOn", "2026-07-01"))
                .andExpect(status().isCreated());

        MvcResult second = mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(sampleFile()).param("name", "2차").param("startedOn", "2026-07-01"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = bodyOf(second);
        assertTrue(body.contains("\"newPoints\":0"));
        assertTrue(body.contains("\"existingPoints\":49"));
    }

    @Test
    @DisplayName("요청한 조사 기간이 프로젝트에 반영된다")
    void import_periodFollowsRequest() throws Exception {
        MvcResult typed = mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(sampleFile())
                        .param("name", "2026 정기조사")
                        .param("startedOn", "2026-07-01"))
                .andExpect(status().isCreated())
                .andReturn();

        Matcher m = Pattern.compile("\"projectId\":(\\d+)").matcher(bodyOf(typed));
        assertTrue(m.find());
        MvcResult project = mockMvc.perform(get("/api/survey-projects/" + m.group(1)))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(project).contains("\"startedOn\":\"2026-07-01\""));
    }

    @Test
    @DisplayName("엑셀이 아닌 파일을 올리면 400으로 알린다 — 서버 오류로 새지 않게")
    void import_brokenFile_400() throws Exception {
        // zip 서명만 맞고 내용은 엑셀이 아니다
        MockMultipartFile broken = new MockMultipartFile(
                "file", "대상지.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03});

        mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(broken).param("name", "깨진 파일").param("startedOn", "2026-07-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조사명·시작일 없이 업로드하면 400")
    void import_withoutRequiredParams_400() throws Exception {
        mockMvc.perform(multipart("/api/imports/survey-csv").file(sampleFile()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/imports/survey-csv")
                        .file(sampleFile()).param("name", "시작일 누락"))
                .andExpect(status().isBadRequest());
    }

    private int countOf(String body, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = body.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }
}
