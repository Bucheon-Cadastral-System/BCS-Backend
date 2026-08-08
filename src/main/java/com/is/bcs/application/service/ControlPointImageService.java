package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointImageFileResult;
import com.is.bcs.application.dto.StoredControlPointImageFile;
import com.is.bcs.application.dto.UploadControlPointImageCommand;
import com.is.bcs.application.dto.UploadControlPointImageResult;
import com.is.bcs.application.port.in.controlpointimage.GetControlPointImageFileUseCase;
import com.is.bcs.application.port.in.controlpointimage.GetControlPointImagesUseCase;
import com.is.bcs.application.port.in.controlpointimage.UploadControlPointImageUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpointimage.ControlPointImageFileStoragePort;
import com.is.bcs.application.port.out.controlpointimage.DeleteControlPointImagePort;
import com.is.bcs.application.port.out.controlpointimage.LoadControlPointImagePort;
import com.is.bcs.application.port.out.controlpointimage.SaveControlPointImagePort;
import com.is.bcs.application.port.out.member.LoadMemberPort;
import com.is.bcs.application.port.out.survey.LoadSurveyProjectPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpointimage.ControlPointImage;
import com.is.bcs.domain.controlpointimage.exception.ControlPointImageNotFoundException;
import com.is.bcs.domain.controlpointimage.exception.ControlPointImageStorageException;
import com.is.bcs.domain.member.Member;
import com.is.bcs.domain.member.MemberStatus;
import com.is.bcs.domain.member.exception.InvalidMemberStateException;
import com.is.bcs.domain.member.exception.MemberNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyProjectNotFoundException;
import com.is.bcs.domain.survey.exception.SurveyTargetNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ControlPointImageService implements UploadControlPointImageUseCase, GetControlPointImageFileUseCase
                                                , GetControlPointImagesUseCase {

    private final LoadSurveyProjectPort loadSurveyProjectPort;
    private final LoadControlPointPort loadControlPointPort;
    private final LoadSurveyTargetPort loadSurveyTargetPort;
    private final LoadMemberPort loadMemberPort;

    private final LoadControlPointImagePort loadControlPointImagePort;
    private final SaveControlPointImagePort saveControlPointImagePort;
    private final DeleteControlPointImagePort deleteControlPointImagePort;

    private final ControlPointImageFileStoragePort fileStoragePort;

    private static final Pattern STORED_FILE_NAME_PATTERN =
            Pattern.compile(
                    "^(.+)_"
                            + "[0-9a-fA-F]{8}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{12}"
                            + "\\.webp$");

    @Override
    public UploadControlPointImageResult uploadOrReplace(UploadControlPointImageCommand command) {
        requireProject(command.projectId());

        ControlPoint point = requirePoint(command.pointId());

        lockTarget(command.projectId(), command.pointId());

        requireActiveMember(command.uploaderId());

        ControlPointImage existing = loadControlPointImagePort
                .findByProjectIdAndPointId(command.projectId(), command.pointId())
                .orElse(null);

        boolean created = existing == null;

        /*
         * 기존 파일에 직접 덮어쓰지 않는다.
         * 항상 새 UUID 파일을 먼저 만든 뒤 DB가 새 파일을 가리키게 한다.
         */
        StoredControlPointImageFile storedFile =
                fileStoragePort.store(
                        command.projectId(),
                        command.pointId(),
                        point.getName(),
                        command.capturedAt(),
                        command.originalFileName(),
                        command.contentType(),
                        command.fileSize(),
                        command.content()
                );

        try {
            /*
             * 기존 DB 행이 있으면 명시적으로 DELETE를 flush한 뒤 새 행을 넣는다.
             * 그렇지 않으면 (project_id, point_id) 유니크 제약 때문에
             * INSERT가 DELETE보다 먼저 실행될 수 있다.
             */
            if (existing != null) {
                deleteControlPointImagePort.deleteByIdAndFlush(existing.getId());
            }

            ControlPointImage newImage =
                    ControlPointImage.create(
                            command.projectId(),
                            command.pointId(),
                            storedFile.storedFileName(),
                            storedFile.storagePath(),
                            storedFile.originalFileName(),
                            storedFile.contentType(),
                            storedFile.fileSize(),
                            storedFile.width(),
                            storedFile.height(),
                            command.capturedAt(),
                            command.uploaderId()
                    );

            ControlPointImage saved = saveControlPointImagePort.save(newImage);

            registerFileCleanup(
                    storedFile.storagePath(),
                    existing == null ? null : existing.getStoragePath()
            );

            return new UploadControlPointImageResult(
                    saved,
                    created
            );
        } catch (RuntimeException exception) {
            /*
             * DB 저장 또는 flush 전에 실패한 경우에는 트랜잭션 동기화가
             * 등록되지 않았으므로 여기서 새 파일을 즉시 정리한다.
             */
            deleteNewFileQuietly(storedFile.storagePath());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ControlPointImageFileResult getFile(Long imageId, Long requesterId) {
        requireActiveMember(requesterId);

        ControlPointImage image = loadControlPointImagePort
                .findById(imageId)
                .orElseThrow(() -> new ControlPointImageNotFoundException("현장 이미지를 찾을 수 없습니다: " + imageId));

        byte[] content = fileStoragePort.read(image.getStoragePath());

        if (content.length != image.getFileSize()) {
            throw new ControlPointImageStorageException("저장된 이미지 파일 크기가 DB 정보와 일치하지 않습니다.");
        }

        return new ControlPointImageFileResult(
                content,
                image.getContentType(),
                image.getFileSize(),
                toDownloadFileName(image.getStoredFileName())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ControlPointImage> getByPointId(Long pointId, Long requesterId, Pageable pageable) {
        requireActiveMember(requesterId);
        requirePoint(pointId);
        return loadControlPointImagePort.findAllByPointId(pointId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ControlPointImage> getByProjectId(Long projectId, Long requesterId, Pageable pageable) {
        requireActiveMember(requesterId);
        requireProject(projectId);
        return loadControlPointImagePort.findAllByProjectId(projectId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ControlPointImage> getAll(Long requesterId, Pageable pageable) {
        requireActiveMember(requesterId);
        return loadControlPointImagePort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ControlPointImage getByProjectIdAndPointId(Long projectId, Long pointId, Long requesterId) {
        requireActiveMember(requesterId);
        return loadControlPointImagePort.findByProjectIdAndPointId(projectId, pointId)
                .orElseThrow( () ->  new ControlPointImageNotFoundException(projectId, pointId));
    }

    private static String toDownloadFileName(String storedFileName) {
        Matcher matcher = STORED_FILE_NAME_PATTERN.matcher(storedFileName);

        if (!matcher.matches()) {
            return "control-point-image.webp";
        }

        return matcher.group(1) + ".webp";
    }

    private void requireProject(Long projectId) {
        loadSurveyProjectPort.findProjectById(projectId)
                .orElseThrow(() -> new SurveyProjectNotFoundException("조사 프로젝트를 찾을 수 없습니다: " + projectId));
    }

    private ControlPoint requirePoint(Long pointId) {
        return loadControlPointPort.findById(pointId)
                .orElseThrow(() -> new ControlPointNotFoundException("기준점을 찾을 수 없습니다: " + pointId));
    }

    private void lockTarget(Long projectId, Long pointId) {
        boolean lock = loadSurveyTargetPort.lockByProjectIdAndPointId(projectId, pointId);

        if (!lock) {
            throw new SurveyTargetNotFoundException("프로젝트의 조사 대상이 아닌 기준점입니다: " + pointId);
        }
    }

    private void requireActiveMember(Long memberId) {
        Member member = loadMemberPort.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InvalidMemberStateException("활성 회원만 현장 이미지에 접근할 수 있습니다.");
        }
    }

    /**
     * DB와 파일시스템은 하나의 트랜잭션으로 묶이지 않는다.
     *
     * 커밋 성공:
     * - 새 파일 유지
     * - 기존 파일 삭제
     *
     * 롤백:
     * - 기존 DB 행과 기존 파일 유지
     * - 새 파일 삭제
     */
    private void registerFileCleanup(String newStoragePath, String oldStoragePath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (oldStoragePath != null) {
                            deleteOldFileQuietly(oldStoragePath);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            deleteNewFileQuietly(newStoragePath);
                        }
                    }
                }
        );
    }

    private void deleteNewFileQuietly(String storagePath) {
        try {
            fileStoragePort.deleteIfExists(storagePath);
        } catch (RuntimeException cleanupException) {
            log.error("롤백 대상 새 이미지 파일을 삭제하지 못했습니다: {}", storagePath, cleanupException);
        }
    }

    private void deleteOldFileQuietly(String storagePath) {
        try {
            fileStoragePort.deleteIfExists(storagePath);
        } catch (RuntimeException cleanupException) {
            /*
             * DB는 이미 커밋된 상태이므로 요청을 다시 실패시켜서는 안 된다.
             * 고아 파일 정리 대상이므로 경로를 로그에 남긴다.
             */
            log.error("교체 전 이미지 파일을 삭제하지 못했습니다: {}", storagePath, cleanupException);
        }
    }
}