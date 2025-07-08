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
import lombok.NoArgsConstructor;

@Table(name = "lessons")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long lessonId;

	@Column(nullable = false)
	private Long lessonLeader;

	@Column(nullable = false, length = 20)
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
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Column(nullable = true)
	private LocalDateTime openTime;

	@Column(nullable = false)
	private Boolean openRun;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

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
}
