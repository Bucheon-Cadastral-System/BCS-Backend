package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.application.port.in.controlpoint.DeleteControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.UpdateControlPointUseCase;
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
import org.springframework.web.util.UriComponentsBuilder;

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
        return ResponseEntity
                // 관리번호는 형식 미확정 입력이라 경로 세그먼트로 인코딩해 Location을 만든다
                .created(UriComponentsBuilder.fromPath("/api/control-points/{pointNo}")
                        .buildAndExpand(result.point().getPointNo()).encode().toUri())
                .body(body);
    }

    @GetMapping
    public ContentResponse<ControlPointResponse> list() {
        return new ContentResponse<>(getControlPointsUseCase.getAll().stream()
                .map(ControlPointResponse::from)
                .toList());
    }

    @GetMapping("/{pointNo}")
    public ControlPointResponse getByPointNo(@PathVariable("pointNo") String pointNo) {
        return ControlPointResponse.from(getControlPointsUseCase.getByPointNo(pointNo));
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
    /** 최종조사원 — 점 하나를 고른 뒤에만 필요해서 목록과 따로 읽는다. */
    @GetMapping("/{pointId}/last-surveyor")
    public LastSurveyorResponse lastSurveyor(@PathVariable("pointId") Long pointId) {
        return new LastSurveyorResponse(getControlPointsUseCase.getLastSurveyorName(pointId));
    }

    @GetMapping("/{pointId}/usage")
    public ControlPointUsageResponse usage(@PathVariable("pointId") Long pointId) {
        return new ControlPointUsageResponse(deleteControlPointUseCase.isReferenced(pointId));
    }
}
