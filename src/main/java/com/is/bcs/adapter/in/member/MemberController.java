package com.is.bcs.adapter.in.member;

import com.is.bcs.adapter.in.security.oauth2.BcsOAuth2Principal;
import com.is.bcs.application.port.in.member.CompleteMemberProfileUseCase;
import com.is.bcs.application.port.in.member.GetMemberStateUseCase;
import com.is.bcs.application.port.in.member.GetMyProfileUseCase;
import com.is.bcs.application.port.in.member.UpdateMemberProfileUseCase;
import com.is.bcs.application.port.out.token.AccessTokenClaims;
import com.is.bcs.domain.member.District;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.Position;
import com.is.bcs.domain.member.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "2. MemberController", description = "회원 정보 및 가입 신청")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    public static final String CSRF_SECURITY_SCHEME = "CSRF Token";
    private final CompleteMemberProfileUseCase completeMemberProfileUseCase;
    private final GetMemberStateUseCase getMemberStateUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMemberProfileUseCase updateMemberProfileUseCase;

    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/me")
    public ResponseEntity<MemberProfileResponse> getMyProfile(Authentication authentication) {
        AccessTokenClaims principal = (AccessTokenClaims) authentication.getPrincipal();
        Long memberId = principal.memberId();

        GetMyProfileUseCase.Result result = getMyProfileUseCase.getProfile(memberId);

        return ResponseEntity.ok(MemberProfileResponse.from(result));
    }

    @Operation(summary = "가입 정보 입력, CSRF 토큰을 꼭 넣으셔야합니다. (세션 사용하기 때문)", security = @SecurityRequirement(name = CSRF_SECURITY_SCHEME))
    @PutMapping("/me/registration")
    public ResponseEntity<Void> completeRegistration(Authentication authentication, @Valid @RequestBody CompleteRegistrationRequest request
    ) {
        BcsOAuth2Principal principal =(BcsOAuth2Principal) authentication.getPrincipal();
        Long memberId = principal != null ? principal.getMemberId() : null;

        completeMemberProfileUseCase.complete(
                memberId,
                request.toCommand()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 정보 변경 (ACTIVE 유저만)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/me/update")
    public ResponseEntity<Void> updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        AccessTokenClaims principal = (AccessTokenClaims) authentication.getPrincipal();
        Long memberId = principal.memberId();

        updateMemberProfileUseCase.update(
                memberId,
                request.toCommand()
        );
        return ResponseEntity.noContent().build();

    }

    @Operation(summary = "세션을 사용하는 경우 CSRF 토큰 필요, 해당 API : /me/registration (가입 정보 입력) ")
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @Operation(summary = "내 가입 상태 조회")
    @GetMapping("/me/state")
    public ResponseEntity<MemberStateResponse> getMyState(Authentication authentication) {
        BcsOAuth2Principal principal =(BcsOAuth2Principal) authentication.getPrincipal();
        Long memberId = principal != null ? principal.getMemberId() : null;

        GetMemberStateUseCase.Result result = getMemberStateUseCase.getState(memberId);

        return ResponseEntity.ok(
                new MemberStateResponse(
                        result.status(),
                        result.profileCompleted()
                )
        );
    }

    public record CompleteRegistrationRequest(
            @NotBlank
            String name,

            @NotBlank
            @Pattern(regexp = "^01[016789]\\d{7,8}$")
            String phone,

            @NotBlank
            @Email
            String email,

            @NotNull
            District district,

            @NotNull
            Team team,

            @NotNull
            Position position
    ) {
        CompleteMemberProfileUseCase.Command toCommand() {
            return new CompleteMemberProfileUseCase.Command(
                    name,
                    phone,
                    email,
                    district,
                    team,
                    position
            );
        }
    }

    public record UpdateProfileRequest(
            @NotBlank
            @Pattern(regexp = "^01[016789]\\d{7,8}$")
            String phone,

            @NotNull
            District district,

            @NotNull
            Team team,

            @NotNull
            Position position
    ) {
        UpdateMemberProfileUseCase.Command toCommand() {
            return new UpdateMemberProfileUseCase.Command(
                    phone,
                    district,
                    team,
                    position
            );
        }
    }

    public record MemberStateResponse(
            MemberStatus status,
            boolean profileCompleted
    ) {
    }
}
