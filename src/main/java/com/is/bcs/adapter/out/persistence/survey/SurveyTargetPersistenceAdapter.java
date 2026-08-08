package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.survey.DeleteSurveyTargetPort;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.domain.survey.SurveyTarget;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 조사 대상 영속 어댑터 — 조사기록 어댑터와 분리한다.
 * saveAll(List&lt;SurveyTarget&gt;)이 조사기록의 saveAll(List&lt;SurveyRecord&gt;)과 제네릭 소거로 시그니처가 겹쳐 한 클래스에 둘 수 없다.
 */
@Component
@RequiredArgsConstructor
public class SurveyTargetPersistenceAdapter implements LoadSurveyTargetPort, SaveSurveyTargetPort, DeleteSurveyTargetPort {

    // 연관을 껍데기 참조로 만들려면 EntityManager 가 필요하다 — 저장 경로가 상대 행을 읽지 않게 한다
    @PersistenceContext
    private EntityManager entityManager;


    private final SurveyTargetJpaRepository targetRepository;

    @Override
    public long countByProjectId(Long projectId) {
        return targetRepository.countByProjectId(projectId);
    }

    @Override
    public List<Long> findPointIdsByProjectId(Long projectId) {
        return targetRepository.findPointIdsByProjectId(projectId);
    }

    @Override
    public boolean existsByPointId(Long pointId) {
        return targetRepository.existsByPointId(pointId);
    }

    @Override
    public Map<Long, Long> countTargetsByProject() {
        return targetRepository.countByProject().stream()
                .collect(Collectors.toMap(
                        SurveyTargetJpaRepository.ProjectCount::getProjectId,
                        SurveyTargetJpaRepository.ProjectCount::getCnt));
    }

    @Override
    public SurveyTarget save(SurveyTarget target) {
        return targetRepository.save(SurveyTargetJpaEntity.fromDomain(target, entityManager)).toDomain();
    }

    @Override
    public List<SurveyTarget> saveAll(List<SurveyTarget> targets) {
        return targetRepository.saveAll(targets.stream().map(t -> SurveyTargetJpaEntity.fromDomain(t, entityManager)).toList())
                .stream().map(SurveyTargetJpaEntity::toDomain).toList();
    }

    // 파생 삭제는 엔티티를 읽어 하나씩 지운다 — 벌크 JPQL 이면 추가 열(@ElementCollection) 행이 남아 FK 에 걸린다
    @Override
    public void deleteByProjectId(Long projectId) {
        targetRepository.deleteByProjectId(projectId);
    }

    @Override
    public void deleteByProjectIdAndPointIds(Long projectId, List<Long> pointIds) {
        // PostgreSQL 바인드 변수 상한(65,535)에 여유를 두고 나눈다 — 점 조회 어댑터와 같은 규칙
        for (int from = 0; from < pointIds.size(); from += CHUNK_SIZE) {
            targetRepository.deleteByProjectIdAndPointIdIn(
                    projectId, pointIds.subList(from, Math.min(from + CHUNK_SIZE, pointIds.size())));
        }
    }

    @Override
    public boolean lockByProjectIdAndPointId(Long projectId, Long pointId) {
        return targetRepository
                .findByProjectIdAndPointIdForUpdate(projectId, pointId)
                .isPresent();
    }

    private static final int CHUNK_SIZE = 1_000;
}
