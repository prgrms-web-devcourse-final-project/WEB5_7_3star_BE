package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.admin.entity.Category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 레슨 생성 요청 데이터
 */
public record LessonCreateRequestDto(

	@NotBlank(message = "레슨 이름은 필수입니다.")
	@Size(max = 50, message = "레슨명은 5m0자 이내여야 합니다.")
	String lessonName,

	@NotBlank(message = "레슨 설명은 필수입니다.")
	@Size(max = 255, message = "레슨 설명은 255자 이내여야 합니다.")
	String description,

	@NotNull(message = "카테고리는 필수입니다.")
	Category category,

	@NotNull(message = "가격은 필수입니다.")
	@Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
	Integer price,

	@NotNull(message = "최대 참가 인원은 필수입니다.")
	@Min(value = 1, message = "최대 참가 인원은 1명 이상이어야 합니다.")
	Integer maxParticipants,

	@NotNull(message = "레슨 시작 시간은 필수입니다.")
	LocalDateTime startAt,

	@NotNull(message = "레슨 종료 시간은 필수입니다.")
	LocalDateTime endAt,

	LocalDateTime openTime,

	@NotNull(message = "선착순 여부는 필수입니다.")
	Boolean openRun,

	@NotBlank(message = "시/도는 필수입니다.")
	@Size(max = 10, message = "시/도는 10자 이하여야 합니다.")
	String city,

	@NotBlank(message = "시/군/구는 필수입니다.")
	@Size(max = 10, message = "시/군/구는 10자 이하여야 합니다.")
	String district,

	@NotBlank(message = "읍/면/동은 필수입니다.")
	@Size(max = 10, message = "읍/면/동은 10자 이하여야 합니다.")
	String dong,

	@NotBlank(message = "상세주소는 필수입니다.")
	@Size(max = 25, message = "상세주소는 25자 이하여야 합니다.")
	String addressDetail,

	@Size(max = 5, message = "이미지는 최대 5장까지 첨부 가능합니다.")
	List<String> lessonImages

) {

	public LessonCreateRequestDto {
		// null 체크 등 추가 검증 로직
		if (lessonImages == null) {
			lessonImages = List.of(); // 빈 리스트로 초기화
		}
	}
}
