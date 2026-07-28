package com.is.bcs.domain.member.exception;

/**
 * 회원 객체가 반드시 만족해야 하는 내부 불변식이 깨진 경우 발생한다.
 *
 * 사용자 입력 오류가 아니라 애플리케이션 로직 또는 저장 데이터의
 * 정합성 문제이므로 외부에는 내부 서버 오류로 응답한다.
 */
public class InvalidMemberInvariantException extends RuntimeException {

    public InvalidMemberInvariantException(String message) {
        super(message);
    }

}