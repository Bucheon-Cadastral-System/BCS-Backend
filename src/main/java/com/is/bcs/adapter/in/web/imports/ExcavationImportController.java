package com.is.bcs.adapter.in.web.imports;

import com.is.bcs.application.dto.ExcavationImportResult;
import com.is.bcs.application.dto.ImportExcavationCsvCommand;
import com.is.bcs.application.port.in.imports.ImportExcavationCsvUseCase;
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
            @RequestParam(value = "note", required = false) String note
    ) throws IOException {
        ExcavationImportResult result = importExcavationCsvUseCase.importCsv(
                new ImportExcavationCsvCommand(name, note, file.getBytes()));

        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + result.projectId()))
                .body(ExcavationImportResponse.from(result));
    }
}
