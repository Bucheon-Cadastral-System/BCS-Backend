package com.is.bcs.domain.controlpoint;

import com.is.bcs.domain.controlpoint.exception.InvalidControlPointException;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 지적기준점 마스터 — 물리적으로 설치된 점의 성과·속성.
 *
 * 식별은 이름이 아니라 관리번호(pointNo)로 한다 — 이름은 중복 가능성이 있어 식별자로 쓰지 않는다.
 * 망실 여부는 이 애그리거트에 없다 — 망실은 시점·프로젝트마다 다른 조사 결과이므로
 * 조사기록(SurveyRecord)이 소유한다.
 * 최종조사*는 그 규칙의 예외가 아니라 최근 조사의 요약(비정규화 표시값)이다 — 권위는 파일·조사기록에 있다.
 */
@Getter
public class ControlPoint {

    private final Long id;
    private final String pointNo; // 관리번호(예: 41192D000001265) — 자연 식별자, 유일
    private final PointType type;

    private String name; // 기준점명(예: 1465공) — 표시용, 중복 가능
    private TmCoordinate tm; // 공식 성과(권위값)
    private GeoCoordinate geo; // 지도 표시용 파생

    private String regionCode; // 법정동 코드(예: 10300)
    private String regionName; // 법정동명(예: 춘의동)
    private String address; // 상세 소재지

    private MarkerMaterial markerMaterial; // 표지 재질
    private InstallType installType; // 설치구분
    private LocalDate installedDate; // 설치일자(원천이 날짜라 LocalDate — 시각으로 바꾸면 UTC 정규화 때 날짜가 밀린다)
    private TraverseInfo traverse; // 도선 정보 — 도근점 외에는 null

    private String lastSurveyResult; // 최종조사내용 — 파일 문구 그대로("망실,안보임" 같은 자유 표기가 실재해 어휘를 강제하지 않는다)
    private LocalDate lastSurveyedOn; // 최종조사일
    private Long lastSurveyedById; // 최종조사원(회원 id) — 파일에는 없는 값이라 앱 내 조사 기록이 채운다(인증 전 null)

    private ControlPoint(
            Long id,
            String pointNo,
            PointType type,
            String name,
            TmCoordinate tm,
            GeoCoordinate geo,
            String regionCode,
            String regionName,
            String address,
            MarkerMaterial markerMaterial,
            InstallType installType,
            LocalDate installedDate,
            TraverseInfo traverse,
            String lastSurveyResult,
            LocalDate lastSurveyedOn,
            Long lastSurveyedById
    ) {
        this.id = id;
        this.pointNo = requireText(pointNo, "관리번호");
        this.type = Objects.requireNonNull(type, "종류는 필수입니다.");
        this.name = requireText(name, "기준점명");
        this.tm = Objects.requireNonNull(tm, "성과 좌표는 필수입니다.");
        this.geo = Objects.requireNonNull(geo, "표시 좌표는 필수입니다.");
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.address = address;
        this.markerMaterial = markerMaterial;
        this.installType = installType;
        this.installedDate = installedDate;
        this.traverse = traverse;
        this.lastSurveyResult = lastSurveyResult;
        this.lastSurveyedOn = lastSurveyedOn;
        this.lastSurveyedById = lastSurveyedById;
    }

    /** 신규 등록(데이터 임포트 포함). */
    public static ControlPoint register(
            String pointNo,
            PointType type,
            String name,
            TmCoordinate tm,
            GeoCoordinate geo,
            String regionCode,
            String regionName,
            String address,
            MarkerMaterial markerMaterial,
            InstallType installType,
            LocalDate installedDate,
            TraverseInfo traverse,
            String lastSurveyResult,
            LocalDate lastSurveyedOn,
            Long lastSurveyedById
    ) {
        return new ControlPoint(
                null, pointNo, type, name, tm, geo,
                regionCode, regionName, address, markerMaterial, installType, installedDate, traverse,
                lastSurveyResult, lastSurveyedOn, lastSurveyedById
        );
    }

    /** DB 데이터를 도메인 객체로 복원한다. */
    public static ControlPoint restore(
            Long id,
            String pointNo,
            PointType type,
            String name,
            TmCoordinate tm,
            GeoCoordinate geo,
            String regionCode,
            String regionName,
            String address,
            MarkerMaterial markerMaterial,
            InstallType installType,
            LocalDate installedDate,
            TraverseInfo traverse,
            String lastSurveyResult,
            LocalDate lastSurveyedOn,
            Long lastSurveyedById
    ) {
        return new ControlPoint(
                id, pointNo, type, name, tm, geo,
                regionCode, regionName, address, markerMaterial, installType, installedDate, traverse,
                lastSurveyResult, lastSurveyedOn, lastSurveyedById
        );
    }

    /**
     * 최종조사 요약을 앱 내 조사 기록으로 갱신한다.
     *
     * <p>파일 임포트가 채우던 자리를 앱에서 남긴 판정이 이어받는다.
     * 기록을 지워 남은 것이 없으면 세 칸을 함께 비운다.
     */
    public void updateLastSurvey(String result, LocalDate surveyedOn, Long surveyedById) {
        this.lastSurveyResult = result;
        this.lastSurveyedOn = surveyedOn;
        this.lastSurveyedById = surveyedById;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidControlPointException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
