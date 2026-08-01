package com.is.bcs.application.port.out.geo;

import com.is.bcs.domain.controlpoint.GeoCoordinate;
import com.is.bcs.domain.controlpoint.TmCoordinate;

/**
 * 성과 좌표(TM)에서 지도 표시용 경위도를 파생한다.
 * 대상지 파일의 기본 양식에는 경위도 열이 없으므로 임포트할 때 서버가 만들어 낸다.
 */
public interface CoordinateTransformer {

    GeoCoordinate toWgs84(TmCoordinate tm);
}
