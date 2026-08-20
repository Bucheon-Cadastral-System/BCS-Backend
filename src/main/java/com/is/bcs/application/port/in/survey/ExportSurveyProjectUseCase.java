package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProjectExportFile;

public interface ExportSurveyProjectUseCase {

    /**
     * 조사의 대상 기준점을 파일 한 장으로 내보낸다.
     *
     * <p>열 이름은 대상지 파일이 쓰는 이름을 그대로 쓰고, 뒤에 그 점의 최종조사 네 열을 더한다.
     */
    SurveyProjectExportFile export(Long projectId);
}
