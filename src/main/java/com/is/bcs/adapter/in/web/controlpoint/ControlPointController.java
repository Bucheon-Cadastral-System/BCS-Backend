package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.application.port.in.controlpoint.DeleteControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.UpdateControlPointUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/control-points")
public class ControlPointController {

    private final RegisterControlPointUseCase registerControlPointUseCase;
    private final UpdateControlPointUseCase updateControlPointUseCase;
    private final DeleteControlPointUseCase deleteControlPointUseCase;
    private final GetControlPointsUseCase getControlPointsUseCase;

    @PostMapping
    public ResponseEntity<RegisterControlPointResponse> register(
            @Valid @RequestBody RegisterControlPointRequest request) {
        RegisterControlPointResult result = registerControlPointUseCase.register(request.toCommand());
        RegisterControlPointResponse body = RegisterControlPointResponse.from(result);
        if (!result.created()) {
            // 임포트와 같은 규칙이라 기존 점 갱신·재사용으로 끝날 수 있다 — 만든 자원이 없으므로 201이 아니다
            return ResponseEntity.ok(body);
        }
        // Location 은 두지 않는다 — 가리킬 단건 조회 경로가 없다. 만든 점은 이 응답 본문에 그대로 실려 있다
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ContentResponse<ControlPointResponse> list() {
        return new ContentResponse<>(getControlPointsUseCase.getAll().stream()
                .map(ControlPointResponse::from)
                .toList());
    }

    // 수정·삭제 경로는 관리번호가 아니라 id 다 — 수정이 관리번호 자체를 바꿀 수 있어 경로 식별자로 쓸 수 없다

    @PutMapping("/{pointId}")
    public UpdateControlPointResponse update(
            @PathVariable("pointId") Long pointId,
            @Valid @RequestBody UpdateControlPointRequest request
    ) {
        return UpdateControlPointResponse.from(updateControlPointUseCase.update(request.toCommand(pointId)));
    }

    @DeleteMapping("/{pointId}")
    public ResponseEntity<Void> delete(@PathVariable("pointId") Long pointId) {
        deleteControlPointUseCase.delete(pointId);
        return ResponseEntity.noContent().build();
    }

    /** 조사 데이터가 참조 중인지 — 화면이 삭제 확인 창을 물음/불가로 갈라 여는 데 쓴다. */
    /** 최종조사 요약 — 점 하나를 고른 뒤에만 필요해서 목록과 따로 읽는다. */
    @GetMapping("/{pointId}/last-survey")
    public LastSurveyResponse lastSurvey(@PathVariable("pointId") Long pointId) {
        return LastSurveyResponse.from(getControlPointsUseCase.getLastSurvey(pointId));
    }

    @GetMapping("/{pointId}/usage")
    public ControlPointUsageResponse usage(@PathVariable("pointId") Long pointId) {
        return new ControlPointUsageResponse(deleteControlPointUseCase.isReferenced(pointId));
    }
}
