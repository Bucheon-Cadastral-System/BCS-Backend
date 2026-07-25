package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.in.imports.ImportExcavationCsvUseCase;
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
public class ExcavationImportController {

    private final ImportExcavationCsvUseCase importExcavationCsvUseCase;

    @PostMapping("/excavation-consultation")
    public ResponseEntity<ExcavationImportResponse> importExcavationCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "note", required = false) String note,
            // 조사 계기는 파일 서식과 별개 축 — 생략하면 이 서식을 쓰던 기존 호출과 같게 굴착협의로 본다
            @RequestParam(value = "type", required = false) SurveyProjectType type
    ) throws IOException {
        ExcavationImportResult result = importExcavationCsvUseCase.importCsv(new ImportExcavationCsvCommand(
                type != null ? type : SurveyProjectType.EXCAVATION_CONSULTATION, name, note, file.getBytes()));

        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + result.projectId()))
                .body(ExcavationImportResponse.from(result));
    }
}
