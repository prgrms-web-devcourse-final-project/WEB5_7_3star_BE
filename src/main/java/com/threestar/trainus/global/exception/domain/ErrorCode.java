package com.threestar.trainus.global.exception.domain;

import org.springframework.http.HttpStatus;

import lombok.Getter;

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
	 * Coupon : 쿠폰 관련 예외처리
	 */

	// 400
	COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "쿠폰 발급 기간이 종료되었습니다."),
	COUPON_NOT_YET_OPEN(HttpStatus.BAD_REQUEST, "아직 발급이 시작되지 않은 쿠폰입니다."),
	COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),

	//404
	COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 쿠폰을 찾을 수 없습니다."),
	PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 프로필을 찾을 수 없습니다"),
	METADATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메타데이터를 찾을 수 없습니다"),
	/*
	 * Coupon : 예외처리
	 */

	/*
	 * Lesson : 레슨 관련 예외처리
	 */
	// 400
	INVALID_LESSON_NAME(HttpStatus.BAD_REQUEST, "레슨 이름이 유효하지 않습니다."),

	INVALID_LESSON_PRICE(HttpStatus.BAD_REQUEST, "레슨 가격이 유효하지 않습니다."),
	INVALID_LESSON_DATE(HttpStatus.BAD_REQUEST, "레슨 날짜가 유효하지 않습니다."),

	/*
	 * User : 유저 관련 예외처리
	 */
	EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
	NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),
	INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
	EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
	EMAIL_SEND_FAILED(HttpStatus.BAD_REQUEST, "이메일 발송을 실패했습니다.");
	//마지막 세미콜론 명시 ;

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
