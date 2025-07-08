package com.threestar.trainus.domain.comment.entity;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.entity.BaseDateEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Table(name = "comments")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(length = 50, nullable = false)
    private String content;

    private Long parentCommentId;

    @Column(nullable = false)
    private Boolean deleted;

}
