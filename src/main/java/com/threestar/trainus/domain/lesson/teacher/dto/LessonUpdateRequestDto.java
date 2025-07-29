package com.threestar.trainus.domain.lesson.teacher.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 레슨 수정 요청 데이터
 */
public record LessonUpdateRequestDto(

	@Size(max = 50, message = "레슨명은 50자 이내여야 합니다.")
	String lessonName,

	@Size(max = 255, message = "레슨 설명은 255자 이내여야 합니다.")
	String description,

	Category category,

	@Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
	Integer price,

	@Min(value = 1, message = "최대 참가 인원은 1명 이상이어야 합니다.")
	@Max(value = 100, message = "최대 참가 인원은 100명 이하여야 합니다.")
	Integer maxParticipants,

	LocalDateTime startAt,

	LocalDateTime endAt,

	LocalDateTime openTime,

	Boolean openRun,

	@Size(max = 10, message = "시/도는 10자 이하여야 합니다.")
	String city,

	@Size(max = 10, message = "시/군/구는 10자 이하여야 합니다.")
	String district,

	@Size(max = 10, message = "읍/면/동은 10자 이하여야 합니다.")
	String dong,

	@Size(max = 10, message = "리는 10자 이하여야 합니다.")
	String ri,

	@Size(max = 25, message = "상세주소는 25자 이하여야 합니다.")
	String addressDetail,

	@Size(max = 5, message = "이미지는 최대 5장까지 첨부 가능합니다.")
	List<String> lessonImages

) {

	// 레슨이름, 설명, 이미지는 항상 수정가능
	public boolean hasBasicInfoChanges() {
		return lessonName != null || description != null || (lessonImages != null && !lessonImages.isEmpty());
	}

	// 제한된 필드들은 수정(참가자 없을때만 가능)
	public boolean hasRestrictedChanges() {
		return category != null || price != null || startAt != null || endAt != null || openTime != null
			|| openRun != null || city != null || district != null || dong != null || ri != null || addressDetail != null;
	}

	//시간 관련 필드 수정 체크
	public boolean hasTimeChanges() {
		return startAt != null || endAt != null;
	}
}
