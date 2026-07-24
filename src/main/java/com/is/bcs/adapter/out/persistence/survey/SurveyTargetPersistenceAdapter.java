package com.is.bcs.adapter.out.persistence.survey;

import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.domain.survey.SurveyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조사 대상 영속 어댑터 — 조사기록 어댑터와 분리한다.
 * saveAll(List&lt;SurveyTarget&gt;)이 조사기록의 saveAll(List&lt;SurveyRecord&gt;)과 제네릭 소거로 시그니처가 겹쳐 한 클래스에 둘 수 없다.
 */
@Component
@RequiredArgsConstructor
public class SurveyTargetPersistenceAdapter implements LoadSurveyTargetPort, SaveSurveyTargetPort {

    private final SurveyTargetJpaRepository targetRepository;

    @Override
    public long countByProjectId(Long projectId) {
        return targetRepository.countByProjectId(projectId);
    }

    @Override
    public SurveyTarget save(SurveyTarget target) {
        return targetRepository.save(SurveyTargetJpaEntity.fromDomain(target)).toDomain();
    }

    @Override
    public List<SurveyTarget> saveAll(List<SurveyTarget> targets) {
        return targetRepository.saveAll(targets.stream().map(SurveyTargetJpaEntity::fromDomain).toList())
                .stream().map(SurveyTargetJpaEntity::toDomain).toList();
    }
}
