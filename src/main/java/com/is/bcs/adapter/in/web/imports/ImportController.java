package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.port.in.imports.ImportControlPointsUseCase;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.application.port.in.imports.PreviewControlPointsUseCase;
import com.is.bcs.application.port.in.imports.PreviewSurveyCsvUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/**
 * 파일 등록.
 * 조사 대상지와 기준점은 서식이 비슷하지만 요구하는 열이 달라, 읽어 보는 단계부터 용도별로 갈라 둔다 —
 * 하나로 두면 서버가 무엇으로 쓸 파일인지 몰라 어느 쪽 파일이든 통과시킨다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportSurveyCsvUseCase importSurveyCsvUseCase;
    private final ImportControlPointsUseCase importControlPointsUseCase;
    private final PreviewSurveyCsvUseCase previewSurveyCsvUseCase;
    private final PreviewControlPointsUseCase previewControlPointsUseCase;

    /** 확정 전에 파일만 읽어 본다 — 등록은 일어나지 않으므로 확정할 때 파일을 다시 보낸다. */
    @PostMapping("/survey-csv/preview")
    public ImportPreviewResponse previewSurveyCsv(@RequestPart("file") MultipartFile file) throws IOException {
        return ImportPreviewResponse.from(previewSurveyCsvUseCase.preview(file.getBytes()));
    }

    /** 기준점 서식으로 읽어 본다 — 조사 대상지와 요구하는 열이 달라 결과가 갈린다. */
    @PostMapping("/control-points/preview")
    public ControlPointPreviewResponse previewControlPoints(@RequestPart("file") MultipartFile file) throws IOException {
        return ControlPointPreviewResponse.from(previewControlPointsUseCase.previewControlPoints(file.getBytes()));
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

    /** 조사 없이 기준점만 등록·갱신한다 — 만들어지는 자원이 여럿이라 Location 은 두지 않는다. */
    @PostMapping("/control-points")
    public ResponseEntity<ControlPointImportResponse> importControlPoints(@RequestPart("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ControlPointImportResponse.from(importControlPointsUseCase.importControlPoints(file.getBytes())));
    }
}
