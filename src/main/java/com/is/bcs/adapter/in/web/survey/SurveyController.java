package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.application.port.in.survey.CancelSurveyUseCase;
import com.is.bcs.application.port.in.survey.CreateSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.application.port.in.survey.RecordSurveyUseCase;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/survey-projects")
public class SurveyController {

    private final CreateSurveyProjectUseCase createSurveyProjectUseCase;
    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final RecordSurveyUseCase recordSurveyUseCase;
    private final CancelSurveyUseCase cancelSurveyUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;

    @PostMapping
    public ResponseEntity<SurveyProjectResponse> create(@Valid @RequestBody CreateSurveyProjectRequest request) {
        SurveyProject project = createSurveyProjectUseCase.create(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + project.getId()))
                .body(SurveyProjectResponse.from(project));
    }

    @GetMapping
    public ContentResponse<SurveyProjectResponse> list() {
        return new ContentResponse<>(getSurveyProjectsUseCase.getAll().stream()
                .map(SurveyProjectResponse::from)
                .toList());
    }

    @GetMapping("/{projectId}")
    public SurveyProjectResponse getById(@PathVariable("projectId") Long projectId) {
        return SurveyProjectResponse.from(getSurveyProjectsUseCase.getById(projectId));
    }

    @GetMapping("/{projectId}/progress")
    public SurveyProgressResponse progress(@PathVariable("projectId") Long projectId) {
        return SurveyProgressResponse.from(getSurveyRecordsUseCase.getProgress(projectId));
    }

    @GetMapping("/{projectId}/records")
    public ContentResponse<SurveyRecordResponse> listRecords(@PathVariable("projectId") Long projectId) {
        return new ContentResponse<>(getSurveyRecordsUseCase.getByProjectId(projectId).stream()
                .map(SurveyRecordResponse::from)
                .toList());
    }

    @PutMapping("/{projectId}/records/{pointId}")
    public SurveyRecordResponse record(
            @PathVariable("projectId") Long projectId,
            @PathVariable("pointId") Long pointId,
            @Valid @RequestBody RecordSurveyRequest request
    ) {
        return SurveyRecordResponse.from(recordSurveyUseCase.record(request.toCommand(projectId, pointId)));
    }

    @DeleteMapping("/{projectId}/records/{pointId}")
    public ResponseEntity<Void> cancel(
            @PathVariable("projectId") Long projectId,
            @PathVariable("pointId") Long pointId
    ) {
        cancelSurveyUseCase.cancel(projectId, pointId);
        return ResponseEntity.noContent().build();
    }
}
