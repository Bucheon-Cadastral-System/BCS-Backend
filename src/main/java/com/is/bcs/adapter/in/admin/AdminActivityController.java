package com.is.bcs.adapter.in.admin;

import com.is.bcs.adapter.in.web.exception.InvalidPageRequestException;
import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.domain.admin.AdminActivityType;
import com.is.bcs.domain.page.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. AdminActivityController", description = "관리자 활동 로그")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private final GetAdminActivityUseCase getAdminActivityUseCase;

    @Operation(summary = "관리자 활동 로그 조회")
    @GetMapping
    public ResponseEntity<PageResponse<AdminActivityResponse>> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AdminActivityType activityType
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size);

        GetAdminActivityUseCase.Command command =
                new GetAdminActivityUseCase.Command(activityType);

        Page<AdminActivityResponse> activities = getAdminActivityUseCase
                .getActivities(pageable, command)
                .map(AdminActivityResponse::from);

        return ResponseEntity.ok(PageResponse.from(activities));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("page는 0 이상이어야 합니다. 입력값=" + page);
        }

        if (size < 1 || size > 100) {
            throw new InvalidPageRequestException("size는 1 이상 100 이하여야 합니다. 입력값=" + size);
        }
    }
}