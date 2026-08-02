package com.is.bcs.adapter.in.admin;

import com.is.bcs.adapter.in.security.CurrentMemberIdResolver;
import com.is.bcs.adapter.in.security.oauth2.BcsOAuth2Principal;
import com.is.bcs.adapter.in.web.exception.InvalidPageRequestException;
import com.is.bcs.application.port.in.admin.*;
import com.is.bcs.domain.member.*;
import com.is.bcs.domain.page.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "3. MemberAdminController", description = "어드민 전용 멤버 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class MemberAdminController {

    private final GetMemberAdminUseCase getMemberAdminUseCase;
    private final ApproveMemberAdminUseCase approveMemberAdminUseCase;
    private final RejectMemberAdminUseCase rejectMemberAdminUseCase;
    private final DeactivateMemberAdminUseCase deactivateMemberAdminUseCase;
    private final ActivateMemberAdminUseCase activateMemberAdminUseCase;
    private final UpdateMemberProfileAdminUseCase updateMemberProfileAdminUseCase;
    private final PromoteMemberAdminUseCase promoteMemberAdminUseCase;
    private final DemoteMemberAdminUseCase demoteMemberAdminUseCase;
    private final CurrentMemberIdResolver currentMemberIdResolver;

    @Operation(summary = "전체 회원 조회")
    @GetMapping
    public ResponseEntity<PageResponse<MemberAdminResponse>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) District district,
            @RequestParam(required = false) Team team,
            @RequestParam(required = false) Position position,
            @RequestParam(required = false) MemberStatus memberStatus,
            @RequestParam(required = false) MemberRole memberRole
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validateSortBy(sortBy)));

        GetMemberAdminUseCase.Command command =
                new GetMemberAdminUseCase.Command(
                        name,
                        email,
                        phone,
                        district,
                        team,
                        position,
                        memberStatus,
                        memberRole
                );

        Page<MemberAdminResponse> response = getMemberAdminUseCase
                .getMembers(pageable, command)
                .map(result -> MemberAdminResponse.from(result));

        return ResponseEntity.ok(PageResponse.from(response));
    }


    @Operation(summary = "회원 가입 승인")
    @PatchMapping("/{memberId}/approve")
    public ResponseEntity<Void> approveMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        approveMemberAdminUseCase.approve(adminId, memberId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "회원 가입 거절")
    @PatchMapping("/{memberId}/reject")
    public ResponseEntity<Void> rejectMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        rejectMemberAdminUseCase.reject(adminId, memberId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "회원 비활성화")
    @PatchMapping("/{memberId}/deactivate")
    public ResponseEntity<Void> deactivateMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        deactivateMemberAdminUseCase.deactivate(adminId, memberId);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "회원 활성화")
    @PatchMapping("/{memberId}/activate")
    public ResponseEntity<Void> activateMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        activateMemberAdminUseCase.activate(adminId, memberId);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "회원 개인정보 강제 변경")
    @PatchMapping("/{memberId}/profile")
    public ResponseEntity<Void> updateMemberProfile(Authentication authentication, @PathVariable Long memberId,
                                        @Valid @RequestBody UpdateMemberProfileAdminRequest request) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        updateMemberProfileAdminUseCase.updateProfile(adminId, memberId, request.toCommand());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 관리자 권한 부여")
    @PatchMapping("/{memberId}/role/admin")
    public ResponseEntity<Void> promoteMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        promoteMemberAdminUseCase.promote(adminId, memberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 관리자 권한 회수")
    @PatchMapping("/{memberId}/role/user")
    public ResponseEntity<Void> demoteMember(Authentication authentication, @PathVariable Long memberId) {
        Long adminId = currentMemberIdResolver.resolve(authentication);
        demoteMemberAdminUseCase.demote(adminId, memberId);

        return ResponseEntity.noContent().build();
    }


    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("page는 0 이상이어야 합니다. 입력값=" + page);
        }

        if (size < 1 || size > 100) {
            throw new InvalidPageRequestException("size는 1 이상 100 이하여야 합니다. 입력값=" + size);
        }
    }

    private String validateSortBy(String sortBy) {
        return switch (sortBy) {
            case "name",
                 "email",
                 "district",
                 "team",
                 "position",
                 "memberStatus",
                 "memberRole",
                 "createdAt" -> sortBy;

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 정렬 기준입니다: " + sortBy
            );
        };
    }

}
