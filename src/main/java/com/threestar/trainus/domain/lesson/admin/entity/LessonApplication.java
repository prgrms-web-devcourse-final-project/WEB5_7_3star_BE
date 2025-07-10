package com.threestar.trainus.domain.lesson.admin.entity;

import com.threestar.trainus.domain.user.entity.User;
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
@Table(name = "lesson_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonApplication extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApplicationStatus status;

	@Builder
	public LessonApplication(User user, Lesson lesson) {
		this.user = user;
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

	//TODO: User완성되면 삭제, 일단은 임시로 넣어둠
	public Long getUserId() {
		return this.user != null ? this.user.getId() : null;
	}

}

