package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointCountSummary;
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
import com.is.bcs.application.port.out.survey.LoadSurveyTargetPort;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.ServiceArea;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointInUseException;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                existing.getLastSurveyResult(), existing.getLastSurveyedOn(), existing.getLastSurveyedById());

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

    @Override
    @Transactional(readOnly = true)
    public String getLastSurveyorName(Long pointId) {
        ControlPoint point = loadControlPointPort.findById(pointId)
                .orElseThrow(() -> new ControlPointNotFoundException("기준점을 찾을 수 없습니다: " + pointId));
        Long surveyorId = point.getLastSurveyedById();
        if (surveyorId == null) {
            return null;
        }
        return loadMemberNamesPort.findNamesByIds(Set.of(surveyorId)).get(surveyorId);
    }
}
