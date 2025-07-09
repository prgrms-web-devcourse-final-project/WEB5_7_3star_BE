package com.threestar.trainus.domain.lesson.admin.entity;

/**
 * 레슨 진행 상태를 나태내는 enum
 * - RECRUITING: 모집중 (참가자 모집 단계)
 * - RECRUITMENT_COMPLETED: 모집완료 (정원 달성)
 * - IN_PROGRESS: 진행중 (레슨이 시작되어 진행 중)
 * - COMPLETED: 완료 (레슨 종료)
 * - CANCELLED: 취소 (레슨이 취소됨)
 */
public enum LessonStatus {
	RECRUITING,
	RECRUITMENT_COMPLETED,
	IN_PROGRESS,
	COMPLETED,
	CANCELLED
}
