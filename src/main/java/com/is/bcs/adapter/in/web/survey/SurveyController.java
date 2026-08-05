package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.adapter.in.web.common.OptionalMemberId;
import com.is.bcs.application.port.in.survey.CancelSurveyUseCase;
import com.is.bcs.application.port.in.survey.CreateSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.DeleteSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.application.port.in.survey.RecordSurveyUseCase;
import com.is.bcs.application.port.in.survey.UpdateSurveyProjectUseCase;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final UpdateSurveyProjectUseCase updateSurveyProjectUseCase;
    private final DeleteSurveyProjectUseCase deleteSurveyProjectUseCase;
    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final RecordSurveyUseCase recordSurveyUseCase;
    private final CancelSurveyUseCase cancelSurveyUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;
    private final OptionalMemberId optionalMemberId;

    @PostMapping
    public ResponseEntity<SurveyProjectResponse> create(
            @Valid @RequestBody CreateSurveyProjectRequest request,
            Authentication authentication
    ) {
        SurveyProject project = createSurveyProjectUseCase.create(
                request.toCommand(optionalMemberId.of(authentication)));
        return ResponseEntity
                .created(URI.create("/api/survey-projects/" + project.getId()))
                .body(SurveyProjectResponse.from(project));
    }

    /** 목록은 요약으로 내린다 — 행마다 완료 표시·작성자를 그리는 화면이 진행률을 건별로 다시 묻지 않게. */
    @GetMapping
    public ContentResponse<SurveyProjectSummaryResponse> list() {
        return new ContentResponse<>(getSurveyProjectsUseCase.getSummaries().stream()
                .map(SurveyProjectSummaryResponse::from)
                .toList());
    }

    @GetMapping("/{projectId}")
    public SurveyProjectResponse getById(@PathVariable("projectId") Long projectId) {
        return SurveyProjectResponse.from(getSurveyProjectsUseCase.getById(projectId));
    }

    @PutMapping("/{projectId}")
    public SurveyProjectResponse update(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody UpdateSurveyProjectRequest request
    ) {
        return SurveyProjectResponse.from(updateSurveyProjectUseCase.update(request.toCommand(projectId)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable("projectId") Long projectId) {
        deleteSurveyProjectUseCase.delete(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/progress")
    public SurveyProgressResponse progress(@PathVariable("projectId") Long projectId) {
        return SurveyProgressResponse.from(getSurveyRecordsUseCase.getProgress(projectId));
    }

    /** 조사 대상 점 id 목록 — 화면이 지도·목록을 그 조사의 대상으로만 좁히는 데 쓴다. */
    @GetMapping("/{projectId}/targets")
    public ContentResponse<Long> listTargets(@PathVariable("projectId") Long projectId) {
        return new ContentResponse<>(getSurveyRecordsUseCase.getTargetPointIds(projectId));
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
            @Valid @RequestBody RecordSurveyRequest request,
            Authentication authentication
    ) {
        return SurveyRecordResponse.from(recordSurveyUseCase.record(
                request.toCommand(projectId, pointId, optionalMemberId.of(authentication))));
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
