package com.threestar.trainus.domain.lesson.admin.entity;

import com.threestar.trainus.global.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

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
