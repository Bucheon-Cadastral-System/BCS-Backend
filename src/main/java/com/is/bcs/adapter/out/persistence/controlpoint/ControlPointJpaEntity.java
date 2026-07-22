package com.is.bcs.adapter.out.persistence.controlpoint;

import com.is.bcs.adapter.out.persistence.common.BaseTime;
import com.is.bcs.domain.controlpoint.ControlPoint;
import com.is.bcs.domain.controlpoint.CoordinateSystem;
import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.InstallType;
import com.is.bcs.domain.controlpoint.MarkerMaterial;
import com.is.bcs.domain.controlpoint.PointType;
import com.is.bcs.domain.controlpoint.TmCoordinate;
import com.is.bcs.domain.controlpoint.TraverseInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "control_points",
        schema = "bcs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_control_points_point_no", columnNames = "point_no")
        },
        indexes = {
                @Index(name = "idx_control_points_type", columnList = "type"),
                @Index(name = "idx_control_points_region_code", columnList = "region_code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlPointJpaEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_no", nullable = false, length = 20)
    private String pointNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PointType type;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "crs", nullable = false, length = 20)
    private CoordinateSystem crs;

    // 성과 좌표는 오차 없는 보존을 위해 numeric — 소수 3자리(mm)까지
    @Column(name = "tm_northing", nullable = false, precision = 12, scale = 3)
    private BigDecimal tmNorthing;

    @Column(name = "tm_easting", nullable = false, precision = 12, scale = 3)
    private BigDecimal tmEasting;

    @Column(name = "lng", nullable = false)
    private double lng;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "region_name", length = 30)
    private String regionName;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "marker_material", length = 20)
    private MarkerMaterial markerMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "install_type", length = 20)
    private InstallType installType;

    @Column(name = "installed_date")
    private LocalDate installedDate;

    @Column(name = "traverse_grade", length = 10)
    private String traverseGrade;

    @Column(name = "traverse_line_name", length = 30)
    private String traverseLineName;

    @Column(name = "traverse_line_no", length = 20)
    private String traverseLineNo;

    @Column(name = "traverse_intersection")
    private Boolean traverseIntersection;

    private ControlPointJpaEntity(
            Long id, String pointNo, PointType type, String name,
            CoordinateSystem crs, BigDecimal tmNorthing, BigDecimal tmEasting, double lng, double lat,
            String regionCode, String regionName, String address,
            MarkerMaterial markerMaterial, InstallType installType, LocalDate installedDate,
            String traverseGrade, String traverseLineName, String traverseLineNo, Boolean traverseIntersection
    ) {
        this.id = id;
        this.pointNo = pointNo;
        this.type = type;
        this.name = name;
        this.crs = crs;
        this.tmNorthing = tmNorthing;
        this.tmEasting = tmEasting;
        this.lng = lng;
        this.lat = lat;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.address = address;
        this.markerMaterial = markerMaterial;
        this.installType = installType;
        this.installedDate = installedDate;
        this.traverseGrade = traverseGrade;
        this.traverseLineName = traverseLineName;
        this.traverseLineNo = traverseLineNo;
        this.traverseIntersection = traverseIntersection;
    }

    public static ControlPointJpaEntity fromDomain(ControlPoint point) {
        TraverseInfo traverse = point.getTraverse();
        return new ControlPointJpaEntity(
                point.getId(), point.getPointNo(), point.getType(), point.getName(),
                point.getTm().crs(), point.getTm().northing(), point.getTm().easting(),
                point.getGeo().longitude(), point.getGeo().latitude(),
                point.getRegionCode(), point.getRegionName(), point.getAddress(),
                point.getMarkerMaterial(), point.getInstallType(), point.getInstalledDate(),
                traverse != null ? traverse.grade() : null,
                traverse != null ? traverse.lineName() : null,
                traverse != null ? traverse.lineNo() : null,
                traverse != null ? traverse.intersection() : null
        );
    }

    public ControlPoint toDomain() {
        return ControlPoint.restore(
                id, pointNo, type, name,
                new TmCoordinate(crs, tmNorthing, tmEasting),
                new GeoCoordinate(lng, lat),
                regionCode, regionName, address,
                markerMaterial, installType, installedDate,
                toTraverse()
        );
    }

    /** 도선 항목이 전부 null이면 '도선 정보 없음'(null)으로 복원한다. */
    private TraverseInfo toTraverse() {
        if (traverseGrade == null && traverseLineName == null
                && traverseLineNo == null && traverseIntersection == null) {
            return null;
        }
        return new TraverseInfo(traverseGrade, traverseLineName, traverseLineNo, traverseIntersection);
    }
}
