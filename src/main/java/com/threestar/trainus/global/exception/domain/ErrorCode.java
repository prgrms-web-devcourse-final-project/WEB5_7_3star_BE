package com.threestar.trainus.global.exception.domain;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /*
     * Commons : 공통 예외 처리
     */
    // 400
    INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST, "요청 데이터가 올바르지 않습니다. 입력 데이터를 확인해 주세요."),
    INVALID_PAGINATION_PARAMETER(HttpStatus.BAD_REQUEST,
            "요청 파라미터가 유효하지 않습니다. page는 1 이상, limit는 1 이상 10 이하로 설정 해야 합니다."),

    // 401
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요한 요청입니다. 로그인 해주세요."),

    // 404
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 레슨을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),


    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다."),

    /*
     * Lesson : 레슨 관련 예외처리
     */
    // 400
    INVALID_LESSON_NAME(HttpStatus.BAD_REQUEST, "레슨 이름이 유효하지 않습니다."),
    INVALID_LESSON_PRICE(HttpStatus.BAD_REQUEST, "레슨 가격이 유효하지 않습니다."),
    INVALID_LESSON_DATE(HttpStatus.BAD_REQUEST, "레슨 날짜가 유효하지 않습니다.");

    /*
     * Lesson : 예외처리
     */

    //마지막 세미콜론 명시 ;


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
