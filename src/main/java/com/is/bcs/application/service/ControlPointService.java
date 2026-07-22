package com.is.bcs.application.service;

import com.is.bcs.application.dto.RegisterControlPointCommand;
import com.is.bcs.application.port.in.controlpoint.GetControlPointsUseCase;
import com.is.bcs.application.port.in.controlpoint.RegisterControlPointUseCase;
import com.is.bcs.application.port.out.controlpoint.LoadControlPointPort;
import com.is.bcs.application.port.out.controlpoint.SaveControlPointPort;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.exception.ControlPointNotFoundException;
import com.is.bcs.domain.controlpoint.exception.DuplicateControlPointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ControlPointService implements RegisterControlPointUseCase, GetControlPointsUseCase {

    private final LoadControlPointPort loadControlPointPort;
    private final SaveControlPointPort saveControlPointPort;

    @Override
    public ControlPoint register(RegisterControlPointCommand command) {
        if (loadControlPointPort.existsByPointNo(command.pointNo())) {
            throw new DuplicateControlPointException(
                    "이미 등록된 관리번호입니다: " + command.pointNo());
        }

        ControlPoint point = ControlPoint.register(
                command.pointNo(), command.type(), command.name(),
                new TmCoordinate(command.crs(), command.northing(), command.easting()),
                new GeoCoordinate(command.longitude(), command.latitude()),
                command.regionCode(), command.regionName(), command.address(),
                command.markerMaterial(), command.installType(), command.installedDate(),
                command.traverse()
        );

        return saveControlPointPort.save(point);
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
}
