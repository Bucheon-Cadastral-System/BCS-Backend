package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.dto.SurveyCsvPreviewResult;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.application.port.in.imports.PreviewSurveyCsvUseCase;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class SurveyCsvImportController {

    private final ImportSurveyCsvUseCase importSurveyCsvUseCase;
    private final PreviewSurveyCsvUseCase previewSurveyCsvUseCase;
    private final JsonMapper jsonMapper;

    /** 확정 전에 파일만 읽어 본다 — 등록은 일어나지 않으므로 확정할 때 파일을 다시 보낸다. */
    @PostMapping("/survey-csv/preview")
    public SurveyCsvPreviewResult preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "columnOverrides", required = false) String columnOverrides
    ) throws IOException {
        return previewSurveyCsvUseCase.preview(file.getBytes(), columnOverrides(columnOverrides));
    }

    @PostMapping("/survey-csv")
    public ResponseEntity<SurveyCsvImportResponse> importSurveyCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("startedOn") LocalDate startedOn,
            @RequestParam(value = "endedOn", required = false) LocalDate endedOn,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "columnOverrides", required = false) String columnOverridesJson
    ) throws IOException {
        SurveyCsvImportResult result = importSurveyCsvUseCase.importCsv(
                new ImportSurveyCsvCommand(
                        name, startedOn, endedOn, note, file.getBytes(), columnOverrides(columnOverridesJson)));

        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + result.projectId()))
                .body(SurveyCsvImportResponse.from(result));
    }

    /**
     * 담당자가 고친 열 매핑 — multipart 요청이라 Map 을 파라미터로 표현할 수 없어 JSON 문자열 하나로 받는다.
     * 형식: {"파일의 열 이름": "읽어 들일 항목"}
     */
    private Map<String, String> columnOverrides(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JacksonException e) {
            throw new InvalidControlPointException("열 매핑 형식이 올바르지 않습니다.");
        }
    }
}
