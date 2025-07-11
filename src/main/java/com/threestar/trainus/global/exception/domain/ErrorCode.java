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
	COUPON_BE_EXHAUSTED(HttpStatus.BAD_REQUEST, "수량이 소진된 쿠폰입니다."),

	//404
	COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 쿠폰을 찾을 수 없습니다."),

	//409
	COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),

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
	INVALID_APPLICATION_ACTION(HttpStatus.BAD_REQUEST, "유효하지 않은 동작입니다. (APPROVED, DENIED)"),
	INVALID_LESSON_STATUS(HttpStatus.BAD_REQUEST,
		"유효하지 않은 레슨 상태입니다. (RECRUITING, RECRUITMENT_COMPLETED, IN_PROGRESS, COMPLETED, CANCELLED)"),
	LESSON_CREATOR_CANNOT_APPLY(HttpStatus.BAD_REQUEST, "레슨 개설자는 자신이 개설한 레슨에 참여 신청할 수 없습니다."),
	CANNOT_CANCEL_APPROVED_APPLICATION(HttpStatus.BAD_REQUEST, "승인된 신청은 취소할 수 없습니다."),
	// 403 Forbidden
	LESSON_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "레슨 삭제 권한이 없습니다. 강사만 삭제할 수 있습니다."),
	LESSON_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "레슨 접근 권한이 없습니다. 강사만 조회할 수 있습니다."),

	//404
	LESSON_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 레슨 신청을 찾을 수 없습니다."),

	// 409 Conflict
	LESSON_APPLICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 레슨 신청입니다."),
	DUPLICATE_LESSON(HttpStatus.CONFLICT, "동일한 이름과 시간으로 이미 생성된 레슨이 있습니다."),
	LESSON_TIME_OVERLAP(HttpStatus.CONFLICT, "해당 시간대에 이미 다른 레슨이 예정되어 있습니다."),
	LESSON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "신청 불가능한 상태의 레슨입니다."),

	// 409
	ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 신청한 레슨입니다."),
	/*
	 * Review : 리뷰 관련 예외처리
	 */
	//400
	INVALID_REVIEW_DATE(HttpStatus.BAD_REQUEST, "레슨 작성 가능 기간이 아닙니다."),
	INVALID_LESSON_PARTICIPANT(HttpStatus.BAD_REQUEST, "올바른 레슨 참여자가 아닙니다."),
	INVALID_REVIEW_COUNT(HttpStatus.BAD_REQUEST, "이미 해당 리뷰를 작성했습니다."),

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

	EMAIL_SEND_FAILED(HttpStatus.BAD_REQUEST, "이메일 발송을 실패했습니다."),

	/*
	 * Profile : 프로필 관련 예외처리
	 */
	PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 프로필을 찾을 수 없습니다"),
	METADATA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메타데이터를 찾을 수 없습니다");

	//마지막 세미콜론 명시 ;

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
