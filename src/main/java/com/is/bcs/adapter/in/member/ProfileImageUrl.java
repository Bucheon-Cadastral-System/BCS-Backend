package com.is.bcs.adapter.in.member;

import com.is.bcs.application.port.in.member.GetMyProfileUseCase;

/**
 * 화면이 프로필 이미지를 받아 갈 경로.
 *
 * <p>등록한 이미지가 없으면 null 이다 — 화면이 없는 이미지를 조회하지 않게 한다.
 */
final class ProfileImageUrl {

    private ProfileImageUrl() {
    }

    static String of(GetMyProfileUseCase.Result result) {
        if (!result.profileImageRegistered()) {
            return null;
        }

        return "/api/members/%d/profile-image".formatted(result.id());
    }
}
