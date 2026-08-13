package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.LastSurveySummary;
import com.is.bcs.application.dto.PointLastSurvey;
import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.application.dto.UpdateControlPointCommand;
import com.is.bcs.application.dto.UpdateControlPointResult;
import com.is.bcs.application.port.in.controlpoint.DeleteControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.application.port.in.controlpoint.UpdateControlPointUseCase;
import com.is.bcs.application.port.out.controlpoint.DeleteControlPointPort;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import com.is.bcs.application.port.out.member.LoadMemberNamesPort;
import com.is.bcs.application.port.out.survey.LoadSurveyRecordPort;
import com.is.bcs.domain.survey.SurveyRecord;
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.ServiceArea;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointInUseException;
import com.is.bcs.domain.controlpoint.exception.ControlPointModifiedException;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ControlPointService implements RegisterControlPointUseCase, UpdateControlPointUseCase,
        DeleteControlPointUseCase, GetControlPointsUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;
    private final DeleteControlPointPort deleteControlPointPort;
    private final ControlPointRegistrar controlPointRegistrar;
    private final CoordinateTransformer coordinateTransformer;
    private final LoadSurveyTargetPort loadSurveyTargetPort;
    private final LoadSurveyRecordPort loadSurveyRecordPort;
    private final LoadMemberNamesPort loadMemberNamesPort;
    private final Clock clock; // 조사 시각은 순간이고 조사일은 지역의 날짜라 어디 기준인지 알아야 한다

    /**
     * 한 점 등록도 파일 임포트와 같은 규칙을 쓴다 — 같은 이름·종류의 점이 있으면 새 점을 만들지 않고 그 점을 입력 값으로
     * 갱신한다. 경로마다 규칙이 다르면 수동으로 만든 중복이 임포트의 이름·종류 매칭을 비결정으로 만든다.
     * 경위도는 성과(TM)에서 파생한다 — 클라이언트가 보낸 값은 권위값과 어긋날 수 있어 받지 않는다.
     */
    @Override
    public RegisterControlPointResult register(RegisterControlPointCommand command) {
        TmCoordinate tm = new TmCoordinate(command.crs(), command.northing(), command.easting());
        GeoCoordinate geo = ImportFileMapper.deriveGeo(coordinateTransformer, tm);
        // 파일 경로는 셀을 다듬어(trim) 맞추므로 여기도 같은 형태로 — 공백만 다른 값이 갱신으로 오판되지 않게
        Row row = Row.manual(
                command.pointNo().trim(), command.type(), command.name().trim(), tm, geo,
                command.regionCode(), command.regionName(), command.address(),
                command.markerMaterial(), command.installType(), command.installedDate(), command.traverse());

        ControlPointRegistrar.Result result = controlPointRegistrar.register(List.of(row));

        return new RegisterControlPointResult(
                result.pointOf(row), result.newPoints() == 1, result.updatedPoints() == 1, warningFor(geo));
    }

    /**
     * 기준점 수정 — 식별(관리번호·이름·종류)과 성과만 바꾸고, 화면이 다루지 않는 소재지·설치·최종조사 값은 그대로 둔다.
     * 관리번호와 이름·종류는 다른 점과 겹칠 수 없다 — 겹치면 임포트의 이름·종류 매칭이 비결정이 된다.
     */
    @Override
    public UpdateControlPointResult update(UpdateControlPointCommand command) {
        ControlPoint existing = requirePoint(command.pointId());
        // 화면이 본 판과 지금 판이 다르면 그사이 누가 먼저 고친 것이다. 덮지 않고 거절한다
        if (command.version() != existing.getVersion()) {
            throw new ControlPointModifiedException(
                    "다른 사람이 먼저 이 기준점을 수정했습니다. 최신 내용을 다시 불러와 주세요.");
        }

        String pointNo = command.pointNo().trim();
        String name = command.name().trim();
        loadControlPointPort.findByPointNo(pointNo)
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> {
                    throw new DuplicateControlPointException(
                            "다른 기준점에 등록된 관리번호입니다: " + pointNo + " (" + other.getName() + ")");
                });
        loadControlPointPort.findByNameAndType(name, command.type())
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> {
                    throw new DuplicateControlPointException(
                            "같은 이름·종류의 기준점이 등록되어 있습니다(관리번호 " + other.getPointNo() + ").");
                });

        TmCoordinate tm = new TmCoordinate(command.crs(), command.northing(), command.easting());
        GeoCoordinate geo = ImportFileMapper.deriveGeo(coordinateTransformer, tm);
        ControlPoint updated = ControlPoint.restore(
                existing.getId(), pointNo, command.type(), name, tm, geo,
                existing.getRegionCode(), existing.getRegionName(), existing.getAddress(),
                existing.getMarkerMaterial(), existing.getInstallType(), existing.getInstalledDate(),
                existing.getTraverse(),
                existing.getLastSurveyResult(), existing.getLastSurveyedOn(), existing.getVersion());

        return new UpdateControlPointResult(saveControlPointPort.save(updated), warningFor(geo));
    }

    /** 조사 데이터는 프로젝트 소유라 점 삭제가 지울 수 없다 — 대상·기록이 걸려 있으면 거부한다. */
    @Override
    public void delete(Long pointId) {
        requirePoint(pointId);
        if (referenced(pointId)) {
            // 어느 점인지는 화면이 이미 알고 있다 — 이름을 덧붙이면 확인 창에서 같은 정보가 두 번 선다
            throw new ControlPointInUseException("프로젝트에서 참조 중인 기준점은 삭제할 수 없습니다.");
        }
        deleteControlPointPort.deleteById(pointId);
    }

    /** 화면이 삭제 확인 창을 열기 전에 가부를 가른다 — 최종 판정은 delete() 가 같은 조건으로 다시 한다(경합 대비). */
    @Override
    @Transactional(readOnly = true)
    public boolean isReferenced(Long pointId) {
        requirePoint(pointId);
        return referenced(pointId);
    }

    private boolean referenced(Long pointId) {
        return loadSurveyTargetPort.existsByPointId(pointId) || loadSurveyRecordPort.existsRecordByPointId(pointId);
    }

    private ControlPoint requirePoint(Long pointId) {
        return loadControlPointPort.findById(pointId)
                .orElseThrow(() -> new ControlPointNotFoundException("기준점을 찾을 수 없습니다: " + pointId));
    }

    /** 부천 밖이어도 저장은 한다(관리 지역이 넓어질 수 있다) — 좌표계·성과를 확인하라는 요청만 함께 보낸다. */
    private static String warningFor(GeoCoordinate geo) {
        return ServiceArea.BUCHEON.contains(geo) ? null
                : String.format(Locale.ROOT, "%s 범위 밖 좌표입니다(위도 %.5f, 경도 %.5f). 원점과 성과를 확인해 주세요.",
                        ServiceArea.BUCHEON.name(), geo.latitude(), geo.longitude());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ControlPoint> getAll() {
        return loadControlPointPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ControlPoint getByPointNo(String pointNo) {
        return loadControlPointPort.findByPointNo(pointNo)
                .orElseThrow(() -> new ControlPointNotFoundException(
                        "기준점을 찾을 수 없습니다: " + pointNo));
    }

    @Override
    @Transactional(readOnly = true)
    public ControlPointCountSummary getCountSummary() {
        Map<PointType, Long> stored = loadControlPointPort.countByType();
        Map<PointType, Long> countByType = new LinkedHashMap<>();
        long total = 0;
        for (PointType type : PointType.values()) {
            long count = stored.getOrDefault(type, 0L);
            countByType.put(type, count);
            total += count;
        }
        return new ControlPointCountSummary(total, countByType);
    }

    /**
     * 기준점의 최종조사 — 가장 최근에 확인된 상태.
     *
     * <p>저장해 두지 않고 볼 때 계산한다. 후보가 둘이다. 기준점이 든 시드 조사(이 시스템에 올라오기 전까지의
     * 총정리)와 앱이 남긴 조사기록이다. 둘을 같은 축에 놓고 날짜가 늦은 쪽을 고른다.
     * 기록이 있다고 무조건 택하면 안 된다. 대상지 파일 임포트가 기존조사일로 과거 날짜 기록을 만들기 때문에
     * 시드보다 오래된 기록이 실재한다.
     *
     * <p>쓰기가 없으므로 기록을 남기거나 지울 때 기준점을 건드릴 일이 없고, 그래서 잠금도 갱신 경로도 없다.
     * 점 하나짜리 경로이고 그 점의 기록은 조사기록 기본키 앞자리(point_id)로 바로 찾는다.
     */
    @Override
    @Transactional(readOnly = true)
    public LastSurveySummary getLastSurvey(Long pointId) {
        ControlPoint point = loadControlPointPort.findById(pointId)
                .orElseThrow(() -> new ControlPointNotFoundException("기준점을 찾을 수 없습니다: " + pointId));
        // 시드에는 조사원도 비고도 없다. 파일이 그 두 가지를 적어 오지 않는다
        LastSurveySummary seed = new LastSurveySummary(
                point.getLastSurveyResult(), point.getLastSurveyedOn(), null, null);
        return loadSurveyRecordPort.findLatestRecordByPointId(pointId)
                .map(this::toLastSurvey)
                .filter(record -> isNotBefore(record.surveyedOn(), seed.surveyedOn()))
                .orElse(seed);
    }

    /**
     * 조사한 적이 있는 점의 최종조사 — 위 단건 계산을 점 전체로 넓힌 것이다.
     *
     * <p>후보가 둘인 것도 날짜가 늦은 쪽을 고르는 것도 같다. 다만 점마다 질의를 두 번 내면 문장이 점 수의 두 배가
     * 되므로, 시드가 적힌 점과 기록이 있는 점을 각각 한 문장으로 읽어 와 여기서 합친다.
     *
     * <p>조사한 적이 없는 점은 담기지 않는다. 미조사는 상태가 아니라 상태가 없는 것이므로,
     * 화면이 목록에 없는 점을 미조사로 읽는다. 응답 크기도 조사 진척만큼만 늘어난다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PointLastSurvey> getLastSurveys() {
        Map<Long, PointLastSurvey> byPoint = new LinkedHashMap<>();
        loadControlPointPort.findSeedLastSurveys().forEach(seed -> byPoint.put(seed.pointId(), seed));
        loadSurveyRecordPort.findLatestSurveyPerPoint().forEach(latest -> byPoint.merge(
                latest.pointId(),
                latest,
                (seed, record) -> isNotBefore(record.surveyedOn(), seed.surveyedOn()) ? record : seed));
        return List.copyOf(byPoint.values());
    }

    /** 날짜가 같으면 기록을 택한다 — 조사원까지 아는 쪽이 더 자세하다. 한쪽 날짜가 비면 있는 쪽이 이긴다. */
    private static boolean isNotBefore(LocalDate recordDate, LocalDate seedDate) {
        if (seedDate == null) {
            return true;
        }
        return recordDate != null && !recordDate.isBefore(seedDate);
    }

    private LastSurveySummary toLastSurvey(SurveyRecord latest) {
        Long surveyorId = latest.getSurveyedById();
        // 이름은 조사원이 있을 때만 찾는다 — 파일로 들어온 기록과 인증 전에 남긴 기록은 이 칸이 비어 있다
        String surveyorName = surveyorId == null
                ? null
                : loadMemberNamesPort.findNamesByIds(Set.of(surveyorId)).get(surveyorId);
        return new LastSurveySummary(
                latest.getResult().getDisplayName(),
                latest.getSurveyedAt().atZoneSameInstant(clock.getZone()).toLocalDate(),
                surveyorName,
                latest.getNote());
    }
}
