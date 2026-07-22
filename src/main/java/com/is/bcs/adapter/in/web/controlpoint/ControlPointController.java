package com.is.bcs.adapter.in.web.controlpoint;

import com.is.bcs.adapter.in.web.common.ContentResponse;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.domain.controlpoint.ControlPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/control-points")
public class ControlPointController {

    private final RegisterControlPointUseCase registerControlPointUseCase;
    private final GetControlPointsUseCase getControlPointsUseCase;

    @PostMapping
    public ResponseEntity<ControlPointResponse> register(@Valid @RequestBody RegisterControlPointRequest request) {
        ControlPoint point = registerControlPointUseCase.register(request.toCommand());
        return ResponseEntity
                // 관리번호는 형식 미확정 입력이라 경로 세그먼트로 인코딩해 Location을 만든다
                .created(UriComponentsBuilder.fromPath("/api/control-points/{pointNo}")
                        .buildAndExpand(point.getPointNo()).encode().toUri())
                .body(ControlPointResponse.from(point));
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
}
