package com.is.bcs.application.port.in.survey;

import com.is.bcs.application.dto.SurveyProjectExportFile;

public interface ExportSurveyProjectUseCase {

    /**
     * 조사의 대상 기준점을 파일 한 장으로 내보낸다.
     *
     * <p>열은 대상지 파일을 읽을 때 요구하는 열과 같고, 뒤에 최종조사 네 열을 더한다.
     * 같은 열 이름을 쓰므로 내보낸 파일을 그대로 다시 올려 새 조사를 만들 수 있다.
     */
    SurveyProjectExportFile export(Long projectId);
}
