package com.threestar.trainus.domain.lesson.issue;

public class LessonApplyStreamConstant {
	public static final String STREAM_KEY = "lesson:apply:stream";
	public static final String GROUP = "lesson-apply-group";
	public static final String STOCK_PREFIX = "lesson:stock:";
	public static final String STATUS_PREFIX = "lesson:apply:status:";
	public static final String DUPLICATE_PREFIX = "lesson:apply:duplicate:";
	public static final String DIRTY_SET_KEY = "lesson:apply:dirty-set";
	public static final String WAITING_ROOM_KEY = "lesson:apply:waiting-room";
	public static final String STATUS_WAITING = "WAITING";
	public static final String STATUS_PROCESSING = "PROCESSING";
	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_FAIL = "FAIL";
	public static final long STATUS_TTL_MINUTE = 10L; // 비동기 처리 상태
	public static final long DUPLICATE_TTL_MINUTE = 1L; // 중복 신청 방지
}
