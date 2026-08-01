package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.application.port.in.imports.PreviewSurveyCsvUseCase;
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
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class SurveyCsvImportController {

    private final ImportSurveyCsvUseCase importSurveyCsvUseCase;
    private final PreviewSurveyCsvUseCase previewSurveyCsvUseCase;

    /** 확정 전에 파일만 읽어 본다 — 등록은 일어나지 않으므로 확정할 때 파일을 다시 보낸다. */
    @PostMapping("/survey-csv/preview")
    public SurveyCsvPreviewResponse preview(@RequestPart("file") MultipartFile file) throws IOException {
        return SurveyCsvPreviewResponse.from(previewSurveyCsvUseCase.preview(file.getBytes()));
    }

    @PostMapping("/survey-csv")
    public ResponseEntity<SurveyCsvImportResponse> importSurveyCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("startedOn") LocalDate startedOn,
            @RequestParam(value = "endedOn", required = false) LocalDate endedOn,
            @RequestParam(value = "note", required = false) String note
    ) throws IOException {
        SurveyCsvImportResult result = importSurveyCsvUseCase.importCsv(
                new ImportSurveyCsvCommand(name, startedOn, endedOn, note, file.getBytes()));

        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + result.projectId()))
                .body(SurveyCsvImportResponse.from(result));
    }
}
