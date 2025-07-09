package com.threestar.trainus.domain.lesson.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "lessonImages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Column(name = "image_url", nullable = false, length = 255)
	private String imageUrl;

	@Builder
	public LessonImage(Lesson lesson, String imageUrl) {
		this.lesson = lesson;
		this.imageUrl = imageUrl;
	}

}

