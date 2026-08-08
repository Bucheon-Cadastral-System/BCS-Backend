package com.is.bcs.adapter.in.web.controlpointimage;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.adapter.in.web.exception.InvalidPageRequestException;
import com.is.bcs.application.port.in.controlpointimage.GetControlPointImagesUseCase;
import com.is.bcs.adapter.in.web.common.OffsetPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ControlPointImageQueryController {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("capturedAt"), Sort.Order.desc("id"));

    private final GetControlPointImagesUseCase getControlPointImagesUseCase;

    private final CurrentMemberIdResolver currentMemberIdResolver;

    /**
     * 기준점에 등록된 모든 조사 프로젝트의 이미지를
     * 실제 촬영시각 최신순으로 조회한다.
     */
    @GetMapping("/api/control-points/{pointId}/images")
    public OffsetPageResponse<ControlPointImageResponse> listByPoint(
            @PathVariable("pointId") Long pointId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        Pageable pageable = pageable(page, size);

        return OffsetPageResponse.from(getControlPointImagesUseCase
                        .getByPointId(pointId, requesterId, pageable)
                        .map(image -> ControlPointImageResponse.from(image)));
    }

    /**
     * 조사 프로젝트에 등록된 모든 기준점 이미지를
     * 실제 촬영시각 최신순으로 조회한다.
     */
    @GetMapping("/api/survey-projects/{projectId}/control-points/{pointId}/image")
    public ControlPointImageResponse getByProjectAndPoint(
            @PathVariable Long projectId,
            @PathVariable Long pointId,
            Authentication authentication
    ) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        return ControlPointImageResponse.from(
                getControlPointImagesUseCase.getByProjectIdAndPointId(
                        projectId,
                        pointId,
                        requesterId
                )
        );
    }

    /**
     * 조사 프로젝트에 등록된 해당 기준점 이미지를
     * 실제 촬영시각 최신순으로 조회한다.
     */
    @GetMapping("/api/survey-projects/{projectId}/images")
    public OffsetPageResponse<ControlPointImageResponse> listByProject(
            @PathVariable("projectId") Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        Pageable pageable = pageable(page, size);

        return OffsetPageResponse.from(
                getControlPointImagesUseCase
                        .getByProjectId(projectId, requesterId, pageable)
                        .map(image -> ControlPointImageResponse.from(image)));
    }

    /**
     * 전체 현장 이미지를 실제 촬영시각 최신순으로 조회한다.
     */
    @GetMapping("/api/control-point-images")
    public OffsetPageResponse<ControlPointImageResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long requesterId = currentMemberIdResolver.resolve(authentication);

        Pageable pageable = pageable(page, size);

        return OffsetPageResponse.from(
                getControlPointImagesUseCase
                        .getAll(requesterId, pageable)
                        .map(image -> ControlPointImageResponse.from(image)));
    }

    private static Pageable pageable(int page, int size) {
        validatePageRequest(page, size);
        return PageRequest.of(
                page,
                size,
                DEFAULT_SORT
        );
    }

    private static void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("page는 0 이상이어야 합니다. 입력값=" + page);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException("size는 1 이상 100 이하여야 합니다. 입력값=" + size);
        }
    }
}