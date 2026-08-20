package com.is.bcs.adapter.in.web.survey;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.adapter.in.web.common.OptionalMemberId;
import com.is.bcs.application.port.in.survey.CancelSurveyUseCase;
import com.is.bcs.application.port.in.survey.CreateSurveyProjectUseCase;
import com.is.bcs.application.dto.SurveyProjectExportFile;
import com.is.bcs.application.port.in.survey.DeleteSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.ExportSurveyProjectUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyProjectsUseCase;
import com.is.bcs.application.port.in.survey.GetSurveyRecordsUseCase;
import com.is.bcs.application.port.in.survey.RecordSurveyUseCase;
import com.is.bcs.application.port.in.survey.UpdateSurveyProjectUseCase;
import com.is.bcs.domain.survey.SurveyProject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.nio.charset.StandardCharsets;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/survey-projects")
public class SurveyController {

    /** xlsx 미디어 타입 — MediaType 상수에 없다 */
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CreateSurveyProjectUseCase createSurveyProjectUseCase;
    private final UpdateSurveyProjectUseCase updateSurveyProjectUseCase;
    private final DeleteSurveyProjectUseCase deleteSurveyProjectUseCase;
    private final GetSurveyProjectsUseCase getSurveyProjectsUseCase;
    private final RecordSurveyUseCase recordSurveyUseCase;
    private final CancelSurveyUseCase cancelSurveyUseCase;
    private final GetSurveyRecordsUseCase getSurveyRecordsUseCase;
    private final ExportSurveyProjectUseCase exportSurveyProjectUseCase;
    private final OptionalMemberId optionalMemberId;

    @PostMapping
    public ResponseEntity<SurveyProjectResponse> create(
            @Valid @RequestBody CreateSurveyProjectRequest request,
            Authentication authentication
    ) {
        SurveyProject project = createSurveyProjectUseCase.create(
                request.toCommand(optionalMemberId.of(authentication)));
        // Location 은 두지 않는다 — 가리킬 단건 조회 경로가 없다. 만든 조사는 이 응답 본문에 그대로 실려 있다
        return ResponseEntity.status(HttpStatus.CREATED).body(SurveyProjectResponse.from(project));
    }

    /** 목록은 요약으로 내린다 — 행마다 완료 표시·작성자를 그리는 화면이 진행률을 건별로 다시 묻지 않게. */
    @GetMapping
    public ContentResponse<SurveyProjectSummaryResponse> list() {
        return new ContentResponse<>(getSurveyProjectsUseCase.getSummaries().stream()
                .map(SurveyProjectSummaryResponse::from)
                .toList());
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

    /** 조사 대상 점 id 목록 — 화면이 지도·목록을 그 조사의 대상으로만 좁히는 데 쓴다. */
    /**
     * 대상 기준점 내보내기 — 대상지 파일과 같은 열에 최종조사를 더한 xlsx.
     *
     * <p>파일 이름은 조사명에서 만들어 헤더로 내린다. 화면이 인증 요청으로 받아 저장하므로 브라우저가
     * 이 헤더를 자동으로 적용하지 않는다 — 이름은 화면이 헤더에서 되읽어 붙인다.
     */
    @GetMapping("/{projectId}/export")
    public ResponseEntity<byte[]> export(@PathVariable("projectId") Long projectId) {
        SurveyProjectExportFile file = exportSurveyProjectUseCase.export(projectId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

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
