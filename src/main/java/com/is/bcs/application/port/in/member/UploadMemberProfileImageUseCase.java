package com.is.bcs.application.port.in.member;

public interface UploadMemberProfileImageUseCase {

    void uploadOrReplace(Command command);

    record Command(
            Long memberId,
            String contentType,
            long fileSize,
            byte[] content
    ) {
    }
}