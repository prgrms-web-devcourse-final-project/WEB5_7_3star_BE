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

	/*
	 * Coupon : 예외처리
	 */

	/*
	 * Lesson : 레슨 관련 예외처리
	 */
	// 400
	INVALID_LESSON_NAME(HttpStatus.BAD_REQUEST, "레슨 이름이 유효하지 않습니다."),
	INVALID_LESSON_PRICE(HttpStatus.BAD_REQUEST, "레슨 가격이 유효하지 않습니다."),
	LESSON_START_TIME_INVALID(HttpStatus.BAD_REQUEST, "레슨 시작 시간이 현재 시간보다 과거입니다."),
	LESSON_END_TIME_BEFORE_START(HttpStatus.BAD_REQUEST, "레슨 종료 시간이 시작 시간보다 빠릅니다."),
	LESSON_DURATION_TOO_SHORT(HttpStatus.BAD_REQUEST, "레슨 시간이 너무 짧습니다. 최소 30분 이상이어야 합니다."),
	LESSON_DURATION_TOO_LONG(HttpStatus.BAD_REQUEST, "레슨 시간이 너무 깁니다. 최대 8시간까지 가능합니다."),
	LESSON_MAX_PARTICIPANTS_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 참가 인원이 허용 범위를 초과했습니다."),
	LESSON_OPEN_TIME_INVALID(HttpStatus.BAD_REQUEST, "선착순 오픈 시간이 레슨 시작 시간 이후입니다."),
	LESSON_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "레슨 이미지는 최대 5장까지 첨부 가능합니다."),
	LESSON_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 레슨 카테고리입니다."),
	INVALID_LESSON_DATE(HttpStatus.BAD_REQUEST, "레슨 날짜가 유효하지 않습니다."),
	LESSON_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 레슨입니다."),
	INVALID_APPLICATION_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 신청 상태입니다. (ALL, PENDING, APPROVED, DENIED)"),

	// 403 Forbidden
	LESSON_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "레슨 삭제 권한이 없습니다. 강사만 삭제할 수 있습니다."),
	LESSON_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "레슨 접근 권한이 없습니다. 강사만 조회할 수 있습니다."),

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
