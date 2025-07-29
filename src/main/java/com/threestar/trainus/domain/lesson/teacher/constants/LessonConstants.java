package com.threestar.trainus.domain.lesson.teacher.constants;

//레슨 관련 상수 정의
public final class LessonConstants {

	private LessonConstants() {
		// Utility class - 인스턴스 생성 방지
	}

	//시간 관련 상수
	public static final class Time {
		//레슨 수정/삭제 제한 시간 (시작 전 12시간)
		public static final int EDIT_DELETE_LIMIT_HOURS = 12;
	}

	//참가자 수 관련 상수
	public static final class Participants {
		//일반 레슨 최대 참가자 수
		public static final int MAX_NORMAL_PARTICIPANTS = 100;

		//선착순 레슨 최대 참가자 수
		public static final int MAX_OPEN_RUN_PARTICIPANTS = 10000;
	}
}
