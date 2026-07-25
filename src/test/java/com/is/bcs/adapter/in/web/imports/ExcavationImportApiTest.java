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

/** 굴착협의 CSV 임포트 API 검증 — DB 필요(bcs/docker-compose). 실파일 업로드로 끝까지 확인한다. */
@SpringBootTest
@Transactional
class ExcavationImportApiTest {

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
        try (var in = getClass().getResourceAsStream("/excavation-sample.csv")) {
            return new MockMultipartFile("file", "굴착협의_대상지.csv", "text/csv", in.readAllBytes());
        }
    }

    @Test
    @DisplayName("실파일 업로드 — 201과 요약(49점·조사 44건), 프로젝트·기준점·기록이 조회된다")
    void importRealFile_endToEnd() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/imports/excavation-consultation")
                        .file(sampleFile())
                        .param("name", "2026 굴착협의")
                        .param("note", "협의번호 2333"))
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
        assertTrue(bodyOf(project).contains("\"type\":\"EXCAVATION_CONSULTATION\""));

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
        mockMvc.perform(multipart("/api/imports/excavation-consultation")
                        .file(sampleFile()).param("name", "1차"))
                .andExpect(status().isCreated());

        MvcResult second = mockMvc.perform(multipart("/api/imports/excavation-consultation")
                        .file(sampleFile()).param("name", "2차"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = bodyOf(second);
        assertTrue(body.contains("\"newPoints\":0"));
        assertTrue(body.contains("\"existingPoints\":49"));
    }

    @Test
    @DisplayName("유형을 지정하면 그 유형으로, 생략하면 굴착협의로 프로젝트가 생긴다")
    void import_projectTypeFollowsRequest() throws Exception {
        MvcResult typed = mockMvc.perform(multipart("/api/imports/excavation-consultation")
                        .file(sampleFile())
                        .param("name", "2026 정기조사")
                        .param("type", "GENERAL"))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m = Pattern.compile("\"projectId\":(\\d+)").matcher(bodyOf(typed));
        assertTrue(m.find());
        MvcResult project = mockMvc.perform(get("/api/survey-projects/" + m.group(1)))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(project).contains("\"type\":\"GENERAL\""));

        MvcResult omitted = mockMvc.perform(multipart("/api/imports/excavation-consultation")
                        .file(sampleFile())
                        .param("name", "2026 굴착협의"))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m2 = Pattern.compile("\"projectId\":(\\d+)").matcher(bodyOf(omitted));
        assertTrue(m2.find());
        MvcResult fallback = mockMvc.perform(get("/api/survey-projects/" + m2.group(1)))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(bodyOf(fallback).contains("\"type\":\"EXCAVATION_CONSULTATION\""));
    }

    @Test
    @DisplayName("조사명 없이 업로드하면 400")
    void import_withoutName_400() throws Exception {
        mockMvc.perform(multipart("/api/imports/excavation-consultation").file(sampleFile()))
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
