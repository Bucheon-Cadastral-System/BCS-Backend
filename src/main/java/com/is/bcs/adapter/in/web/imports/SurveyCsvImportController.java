package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.domain.survey.SurveyProjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class SurveyCsvImportController {

    private final ImportSurveyCsvUseCase importSurveyCsvUseCase;

    @PostMapping("/survey-csv")
    public ResponseEntity<SurveyCsvImportResponse> importSurveyCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "note", required = false) String note,
            // 조사 계기는 파일 서식과 별개 축이라 요청이 정한다
            @RequestParam("type") SurveyProjectType type
    ) throws IOException {
        SurveyCsvImportResult result = importSurveyCsvUseCase.importCsv(
                new ImportSurveyCsvCommand(type, name, note, file.getBytes()));

        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + result.projectId()))
                .body(SurveyCsvImportResponse.from(result));
    }
}
