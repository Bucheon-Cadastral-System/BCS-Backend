package com.is.bcs.adapter.in.security.oauth2;

import com.is.bcs.domain.member.MemberRole;
import com.is.bcs.domain.member.MemberStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class BcsOAuth2Principal implements OAuth2User {

    private final Long memberId;
    private final MemberRole role;
    private final MemberStatus status;
    private final boolean profileCompleted;
    private final Map<String, Object> attributes;

    public BcsOAuth2Principal(
            Long memberId,
            MemberRole role,
            MemberStatus status,
            boolean profileCompleted,
            Map<String, Object> attributes
    ) {
        this.memberId = Objects.requireNonNull(memberId, "회원 ID는 필수입니다.");
        this.role = Objects.requireNonNull(role, "회원 역할은 필수입니다.");
        this.status = Objects.requireNonNull(status, "회원 상태는 필수입니다.");
        this.profileCompleted = profileCompleted;
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "OAuth2 속성은 필수입니다."));

    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(!status.equals(MemberStatus.ACTIVE)) {
            return List.of(new SimpleGrantedAuthority("ROLE_PENDING"));
        }

        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                )
        );
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}