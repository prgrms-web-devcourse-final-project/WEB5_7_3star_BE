package com.threestar.trainus.domain.lesson.admin.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class LessonParticipant extends BaseDateEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long lessonId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson", nullable = false)
	private Lesson lesson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user", nullable = false)
	private User user;

	@CreatedDate
	private LocalDateTime joinAt;
}
