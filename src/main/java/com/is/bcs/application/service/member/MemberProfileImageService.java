package com.is.bcs.application.service.member;

import com.is.bcs.application.port.in.member.DeleteMemberProfileImageUseCase;
import com.is.bcs.application.port.in.member.GetMemberProfileImageUseCase;
import com.is.bcs.application.port.in.member.UploadMemberProfileImageUseCase;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.member.MemberProfileImageStoragePort;
import com.is.bcs.application.port.out.member.SaveMemberPort;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import com.is.bcs.domain.member.exception.MemberProfileImageNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileImageService implements UploadMemberProfileImageUseCase, DeleteMemberProfileImageUseCase,
                                                    GetMemberProfileImageUseCase {

    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final MemberProfileImageStoragePort fileStoragePort;

    @Override
    public void uploadOrReplace(Command command) {
        Member member = requireActiveMemberForUpdate(command.memberId());

        String oldStoragePath = member.getProfileImagePath();

        String newStoragePath =
                fileStoragePort.store(
                        member.getId(),
                        command.contentType(),
                        command.fileSize(),
                        command.content()
                );

        try {
            member.updateProfileImage(newStoragePath);
            saveMemberPort.save(member);
            registerReplacementCleanup(newStoragePath, oldStoragePath); // 오래된 이미지 제거

        } catch (RuntimeException exception) {
            deleteFileQuietly(newStoragePath, "저장 실패 후 신규 프로필 이미지"); // 신규 이미지 제거
            throw exception;
        }
    }

    @Override
    public void delete(Long memberId) {
        Member member = requireActiveMemberForUpdate(memberId);

        String oldStoragePath = member.getProfileImagePath();

        if (oldStoragePath == null) {
            return;
        }

        member.deleteProfileImage();
        saveMemberPort.save(member);

        registerDeleteAfterCommit(oldStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getFile(Long memberId, Long requesterId) {
        requireActiveRequester(requesterId);

        Member member = loadMemberPort
                .findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        String storagePath = member.getProfileImagePath();

        if (storagePath == null || storagePath.isBlank()) {
            throw new MemberProfileImageNotFoundException("등록된 프로필 이미지가 없습니다.");
        }

        byte[] content = fileStoragePort.read(storagePath);

        return new Result(content, WEBP_CONTENT_TYPE, content.length);
    }

    private Member requireActiveMemberForUpdate(Long memberId) {
        Member member = loadMemberPort
                .findByIdForUpdate(memberId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성 회원만 프로필 이미지를 변경할 수 있습니다.");
        }

        return member;
    }

    private void requireActiveRequester(Long requesterId) {
        Member requester = loadMemberPort
                .findById(requesterId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + requesterId));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성 회원만 프로필 이미지를 조회할 수 있습니다.");
        }
    }

    private void registerReplacementCleanup(String newStoragePath, String oldStoragePath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (oldStoragePath != null) {
                            deleteFileQuietly(oldStoragePath, "교체 전 프로필 이미지");
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            deleteFileQuietly(newStoragePath, "롤백 대상 신규 프로필 이미지");
                        }
                    }
                }
        );
    }

    private void registerDeleteAfterCommit(String oldStoragePath) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {deleteFileQuietly(oldStoragePath, "삭제된 프로필 이미지");
                    }
                }
        );
    }

    private void deleteFileQuietly(String storagePath, String description) {
        try {
            fileStoragePort.deleteIfExists(storagePath);
        } catch (RuntimeException exception) {
            log.error(
                    "{} 파일을 정리하지 못했습니다. storagePath={}",
                    description,
                    storagePath,
                    exception
            );
        }
    }
}