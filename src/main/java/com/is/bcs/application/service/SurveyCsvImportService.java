package com.is.bcs.application.service;

import com.is.bcs.application.dto.SurveyCsvImportResult;
import com.is.bcs.application.dto.ImportSurveyCsvCommand;
import com.is.bcs.application.port.in.imports.ImportSurveyCsvUseCase;
import com.is.bcs.application.port.out.survey.SaveSurveyProjectPort;
import com.is.bcs.application.port.out.survey.SaveSurveyRecordPort;
import com.is.bcs.application.port.out.file.TableExtractor;
import com.is.bcs.application.port.out.survey.SaveSurveyTargetPort;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import com.is.bcs.domain.survey.SurveyProject;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.domain.survey.SurveyTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 대상지 파일(CSV·XLSX) 임포트 — 한 파일로 조사 프로젝트 생성, 기준점 마스터 반영, 조사 대상 등록, 기존조사 이력 기록을 수행한다.
 *
 * 파일 파싱은 DB 를 건드리지 않으므로 트랜잭션 밖에서 끝낸다 — 수천 행 xlsx 를 읽는 동안 커넥션을 잡고 있으면
 * 동시에 다른 담당자가 올릴 때 풀이 마른다. 트랜잭션은 실제로 쓰는 구간(store)에만 건다.
 */
@Service
@RequiredArgsConstructor
public class SurveyCsvImportService implements ImportSurveyCsvUseCase {

    private final SaveSurveyProjectPort saveSurveyProjectPort;
    private final SaveSurveyRecordPort saveSurveyRecordPort;
    private final SaveSurveyTargetPort saveSurveyTargetPort;
    private final TableExtractor tableExtractor;
    private final SurveyTargetMapper surveyTargetMapper;
    private final ControlPointRegistrar controlPointRegistrar;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public SurveyCsvImportResult importCsv(ImportSurveyCsvCommand command) {
        // 파싱·검증은 읽기만 하므로 트랜잭션 밖 — 여기서 거부되면 DB 는 시작조차 하지 않는다
        ImportFileMapper.MappingResult mapped = surveyTargetMapper.map(tableExtractor.extract(command.content()));
        mapped.rejectIfAnyRowFailed();

        return transactionTemplate.execute(status -> store(command, mapped.rows()));
    }

    /** 읽어 둔 행을 한 트랜잭션으로 저장한다 — 한 행이라도 실패하면 조사째로 되돌린다. */
    private SurveyCsvImportResult store(ImportSurveyCsvCommand command, List<Row> rows) {
        SurveyProject project = saveSurveyProjectPort.save(SurveyProject.create(
                command.authorId(), command.name(), command.startedOn(), command.endedOn(), command.note()));

        ControlPointRegistrar.Result points = controlPointRegistrar.register(rows);

        // 모든 행은 이 프로젝트의 조사 대상이다 — 진행률의 분모(전체 대상)가 된다.
        // 기본 양식에 없어 기준점 마스터로 옮기지 못한 열은 이 대상에 그대로 보관한다.
        List<SurveyTarget> targets = new ArrayList<>(rows.size());
        List<SurveyRecord> records = new ArrayList<>();
        for (Row row : rows) {
            Long pointId = points.pointOf(row).getId();
            targets.add(SurveyTarget.create(project.getId(), pointId, row.extras()));
            if (row.priorResult() != null) {
                // 조사원은 파일에 없는 값이라 비워 둔다 — 인증이 붙으면 앱 내 기록이 채운다
                records.add(SurveyRecord.create(
                        project.getId(), pointId, row.priorResult(), surveyedAt(row), row.note(), null));
            }
        }
        saveSurveyTargetPort.saveAll(targets);
        saveSurveyRecordPort.saveAll(records);

        return new SurveyCsvImportResult(
                project.getId(), rows.size(), points.newPoints(),
                points.existingPoints(), points.updatedPoints(), records.size());
    }

    /** 조사 시각 = 기존조사일의 KST 자정. 조사일이 비어 있으면 임포트 시각으로 방어한다. */
    private OffsetDateTime surveyedAt(Row row) {
        if (row.priorSurveyDate() != null) {
            return row.priorSurveyDate().atStartOfDay(clock.getZone()).toOffsetDateTime();
        }
        return OffsetDateTime.now(clock);
    }
}
