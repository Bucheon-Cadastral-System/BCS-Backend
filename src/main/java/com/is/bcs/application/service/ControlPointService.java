package com.is.bcs.application.service;

import com.is.bcs.application.dto.ControlPointCountSummary;
import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.dto.RegisterControlPointResult;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.geo.CoordinateTransformer;
import com.is.bcs.application.service.ImportFileMapper.Row;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.ServiceArea;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ControlPointService implements RegisterControlPointUseCase, GetControlPointsUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final ControlPointRegistrar controlPointRegistrar;
    private final CoordinateTransformer coordinateTransformer;

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

        // 부천 밖이어도 등록은 한다(관리 지역이 넓어질 수 있다) — 좌표계·성과를 확인하라는 요청만 함께 보낸다
        String warning = ServiceArea.BUCHEON.contains(geo) ? null
                : String.format(Locale.ROOT, "%s 범위 밖 좌표입니다(위도 %.5f, 경도 %.5f) — 원점과 성과를 확인해 주세요.",
                        ServiceArea.BUCHEON.name(), geo.latitude(), geo.longitude());
        return new RegisterControlPointResult(
                result.pointOf(row), result.newPoints() == 1, result.updatedPoints() == 1, warning);
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
}
