package com.threestar.trainus.domain.review.entity;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "reviews")
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private Float rating;

    @Column(length = 255, nullable = false)
    private String content;

    @Column(length = 255, nullable = false)
    private String image;

    private LocalDateTime deletedAt;

}
