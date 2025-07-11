package com.threestar.trainus.domain.lesson.admin.entity;

import java.time.LocalDateTime;

import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "lessons")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long lessonLeader;

	@Column(nullable = false, length = 50)
	private String lessonName;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false)
	private int maxParticipants;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Column(nullable = false)
	private Integer price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Column(nullable = true)
	private LocalDateTime openTime;

	//false(선착순이 아닌 레슨, 강사가 직접 승인 거절), true -> 선착순 수업
	@Column(nullable = false)
	private Boolean openRun;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LessonStatus status;

	//createdAt
	//updatedAt

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(nullable = false, length = 10)
	private String city;

	@Column(nullable = false, length = 10)
	private String district;

	@Column(nullable = false, length = 10)
	private String dong;

	@Column(nullable = false, length = 25)
	private String addressDetail;

	@Column(nullable = true)
	private Integer participantCount;

	@Builder
	public Lesson(Long lessonLeader, String lessonName, String description,
		Integer maxParticipants, LocalDateTime startAt, LocalDateTime endAt,
		Integer price, Category category, LocalDateTime openTime,
		Boolean openRun, String city, String district, String dong,
		String addressDetail) {
		this.lessonLeader = lessonLeader;
		this.lessonName = lessonName;
		this.description = description;
		this.maxParticipants = maxParticipants;
		this.startAt = startAt;
		this.endAt = endAt;
		this.price = price;
		this.category = category;
		this.openTime = openTime;
		this.openRun = openRun;
		this.city = city;
		this.district = district;
		this.dong = dong;
		this.addressDetail = addressDetail;
		//새로 생성된 레슨은 항상 모집중 상태로 초기값 설정
		this.status = LessonStatus.RECRUITING;
		//새로 생성된 참가자 수도 0명으로 시작하도록 설정
		this.participantCount = 0;
	}

	//레슨을 삭제 상태로 만들어주는 메소드(삭제시간을 현재시간으로)
	public void lessonDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	//레슨이 삭제된 상태인지 확인
	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	// 참가자 수 증가
	public void incrementParticipantCount() {
		this.participantCount++;

		// 정원 달성 시 -> 모집완료로 상태 변경
		if (this.participantCount >= this.maxParticipants) {
			this.status = LessonStatus.RECRUITMENT_COMPLETED;
		}
	}

}
