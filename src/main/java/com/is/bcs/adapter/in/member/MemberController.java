package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.member.CompleteMemberProfileUseCase;
import com.is.bcs.application.port.in.member.GetMemberStateUseCase;
import com.is.bcs.config.SwaggerConfig;
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

    @Operation(summary = "가입 정보 입력", security = @SecurityRequirement(name = CSRF_SECURITY_SCHEME))
    @PutMapping("/me/registration")
    public ResponseEntity<Void> completeRegistration(Authentication authentication, @Valid @RequestBody CompleteRegistrationRequest request
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        completeMemberProfileUseCase.complete(
                memberId,
                request.toCommand()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @Operation(summary = "내 가입 상태 조회")
    @GetMapping("/me/state")
    public ResponseEntity<MemberStateResponse> getMyState(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getName());

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

    public record MemberStateResponse(
            MemberStatus status,
            boolean profileCompleted
    ) {
    }
}