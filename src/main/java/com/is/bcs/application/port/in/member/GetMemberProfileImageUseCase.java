package com.is.bcs.application.port.in.member;

public interface GetMemberProfileImageUseCase {

    Result getFile(Long memberId, Long requesterId);

    record Result(
            byte[] content,
            String contentType,
            long fileSize
    ) {
    }

}