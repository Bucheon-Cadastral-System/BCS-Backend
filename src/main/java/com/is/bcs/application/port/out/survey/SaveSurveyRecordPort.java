package com.is.bcs.application.port.out.survey;

import com.is.bcs.domain.survey.SurveyRecord;

import java.util.List;
import java.util.Optional;

public interface SaveSurveyRecordPort {

    SurveyRecord save(SurveyRecord record);

    List<SurveyRecord> saveAll(List<SurveyRecord> records);

    /**
     * 대상으로 지정된 점이면 기록을 쓰고(이미 있으면 전 필드 정정), 대상이 아니면 아무것도 쓰지 않는다.
     * 대상 확인과 쓰기가 한 문장이라 동시 기록·대상 재지정이 그 사이에 끼지 못한다.
     *
     * @return 저장된 기록. 대상이 아니면 empty — 호출자가 404 로 번역한다.
     */
    Optional<SurveyRecord> upsertForTarget(SurveyRecord record);
}
