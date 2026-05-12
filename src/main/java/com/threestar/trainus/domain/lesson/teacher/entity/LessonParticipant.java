package com.threestar.trainus.domain.lesson.teacher.entity;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.user.entity.User;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "lesson_participants",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_lesson_participants", columnNames = {"user_id", "lesson_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonParticipant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDateTime joinAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ParticipantStatus status;

	@Builder
	public LessonParticipant(Lesson lesson, User user) {
		this.lesson = lesson;
		this.user = user;
		if (lesson.getPrice() == 0) {
			this.status = ParticipantStatus.COMPLETED;
		} else {
			this.status = ParticipantStatus.PAYMENT_PENDING;
		}
	}

	@PrePersist
	private void prePersist() {
		this.joinAt = LocalDateTime.now();
	}

	// 결제 완료 시 상태 변경 메서드
	public void completePayment() {
		if (this.status == ParticipantStatus.PAYMENT_PENDING) {
			this.status = ParticipantStatus.COMPLETED;
		}
	}

	// 결제 대기 상태인지 확인하는 메서드
	public boolean isPaymentPending() {
		return this.status == ParticipantStatus.PAYMENT_PENDING;
	}

	// 참가 완료 상태인지 확인하는 메서드
	public boolean isCompleted() {
		return this.status == ParticipantStatus.COMPLETED;
	}
}
