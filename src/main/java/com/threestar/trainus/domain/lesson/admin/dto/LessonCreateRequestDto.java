package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.admin.entity.Category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LessonCreateRequestDto {

	@NotBlank(message = "레슨 이름은 필수입니다.")
	@Size(max = 20, message = "레슨명은 20자 이내여야 합니다.")
	private String lessonName;

	@NotBlank(message = "레슨 설명은 필수입니다.")
	@Size(max = 255, message = "레슨 설명은 255자 이내여야 합니다.")
	private String description;

	@NotNull(message = "카테고리는 필수입니다.")
	private Category category;

	@NotNull(message = "레슨 가격은 필수입니다.")
	@Min(value = 0, message = "가격은 0원 이상이여야 합니다.")
	private Integer price;

	@NotNull(message = "최대 참가 인원은 필수입니다.")
	@Min(value = 1, message = "최대 참가 인원은 1명 이상이여야 합니다.")
	private Integer maxParticipants;

	@NotNull(message = "레슨 시작 시간은 필수입니다.")
	private LocalDateTime startAt;

	@NotNull(message = "레슨 종료 시간은 필수입니다.")
	private LocalDateTime endAt;

	private LocalDateTime openTime;

	@NotNull(message = "선착순 여부는 필수입니다.")
	private Boolean openRun;

	@NotBlank(message = "시/도는 필수입니다.")
	@Size(max = 10, message = "시/도는 10자 이하여야 합니다.")
	private String city;

	@NotBlank(message = "시/군/구는 필수입니다.")
	@Size(max = 10, message = "시/군/구는 10자 이하여야 합니다.")
	private String district;

	@NotBlank(message = "읍/면/동은 필수입니다.")
	@Size(max = 10, message = "읍/면/동은 10자 이하여야 합니다.")
	private String dong;

	@NotBlank(message = "상세주소는 필수입니다.")
	@Size(max = 25, message = "상세주소는 25자 이하여야 합니다.")
	private String addressDetail;

	@Size(max = 5, message = "이미지는 최대 5장까지 첨부 가능합니다.")
	private List<String> lessonImages;

	@Builder
	public LessonCreateRequestDto(String lessonName, String description, Category category,
		Integer price, Integer maxParticipants, LocalDateTime startAt,
		LocalDateTime endAt, LocalDateTime openTime, Boolean openRun,
		String city, String district, String dong, String addressDetail,
		List<String> lessonImages) {
		this.lessonName = lessonName;
		this.description = description;
		this.category = category;
		this.price = price;
		this.maxParticipants = maxParticipants;
		this.startAt = startAt;
		this.endAt = endAt;
		this.openTime = openTime;
		this.openRun = openRun;
		this.city = city;
		this.district = district;
		this.dong = dong;
		this.addressDetail = addressDetail;
		this.lessonImages = lessonImages;
	}

	public String getLessonName() {
		return lessonName;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public Integer getPrice() {
		return price;
	}

	public Integer getMaxParticipants() {
		return maxParticipants;
	}

	public LocalDateTime getStartAt() {
		return startAt;
	}

	public LocalDateTime getEndAt() {
		return endAt;
	}

	public LocalDateTime getOpenTime() {
		return openTime;
	}

	public Boolean getOpenRun() {
		return openRun;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getDong() {
		return dong;
	}

	public String getAddressDetail() {
		return addressDetail;
	}

	public List<String> getLessonImages() {
		return lessonImages;
	}

}
