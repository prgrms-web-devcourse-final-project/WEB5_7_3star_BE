package com.threestar.trainus.domain.lesson.admin.entity;

import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lessonApplication")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonApplication extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Lesson lesson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApplicationStatus status;

	@Builder

	public LessonApplication(Long userId, Lesson lesson, ApplicationStatus status) {
		this.userId = userId;
		this.lesson = lesson;
		this.status = ApplicationStatus.PENDING;
	}

	//상태 변경 메서드(대기 -> 승인으로 바뀔때)
	public void approve() {
		this.status = ApplicationStatus.APPROVED;
	}

	//상태 변경 메서드(대기 -> 거절으로 바뀔때)
	public void deny() {
		this.status = ApplicationStatus.DENIED;
	}

}

