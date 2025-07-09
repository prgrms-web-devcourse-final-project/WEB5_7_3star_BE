package com.threestar.trainus.domain.lesson.admin.entity;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lesson_participants")
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

	@Builder
	public LessonParticipant(Lesson lesson, User user) {
		this.lesson = lesson;
		this.user = user;
	}

	@PrePersist
	private void prePersist() {
		this.joinAt = LocalDateTime.now();
	}
}
