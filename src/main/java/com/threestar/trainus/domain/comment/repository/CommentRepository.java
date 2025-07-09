package com.threestar.trainus.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
