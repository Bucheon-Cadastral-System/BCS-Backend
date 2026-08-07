package com.is.bcs.adapter.in.admin;

import com.is.bcs.adapter.in.web.exception.InvalidPageRequestException;
import com.is.bcs.application.port.in.admin.GetAdminActivityUseCase;
import com.is.bcs.domain.admin.AdminActivityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AdminActivityController", description = "관리자 활동 로그")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private final GetAdminActivityUseCase getAdminActivityUseCase;
    private final AdminActivityCursorCodec cursorCodec;

    @Operation(summary = "관리자 활동 로그 조회")
    @GetMapping
    public ResponseEntity<KeysetPageResponse<AdminActivityResponse>> getActivities(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AdminActivityType activityType) {

        validateSize(size);

        AdminActivityCursor decodedCursor = cursorCodec.decode(cursor);

        GetAdminActivityUseCase.Command command =
                new GetAdminActivityUseCase.Command(
                        activityType,
                        decodedCursor == null ? null : decodedCursor.createdAt(),
                        decodedCursor == null ? null : decodedCursor.id()
                );

        Slice<GetAdminActivityUseCase.Result> result =
                getAdminActivityUseCase.getActivities(PageRequest.of(0, size), command);

        List<AdminActivityResponse> content = result.getContent()
                .stream()
                .map(AdminActivityResponse::from)
                .toList();

        String nextCursor = createNextCursor(
                result,
                content
        );

        return ResponseEntity.ok(
                new KeysetPageResponse<>(
                        content,
                        nextCursor,
                        result.hasNext(),
                        content.size()
                )
        );
    }

    private String createNextCursor(Slice<GetAdminActivityUseCase.Result> result, List<AdminActivityResponse> content) {
        if (!result.hasNext() || content.isEmpty()) {
            return null;
        }

        AdminActivityResponse last = content.get(content.size() - 1);

        return cursorCodec.encode(
                new AdminActivityCursor(
                        last.createdAt(),
                        last.id()
                )
        );
    }

    private void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw new InvalidPageRequestException("size는 1 이상 100 이하여야 합니다. 입력값=" + size);
        }
    }
}