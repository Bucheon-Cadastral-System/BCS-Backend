package com.is.bcs.application.port.out.member;

public interface MemberProfileImageStoragePort {

    String store(
            Long memberId,
            String contentType,
            long declaredFileSize,
            byte[] content
    );

    byte[] read(String storagePath);

    void deleteIfExists(String storagePath);

}